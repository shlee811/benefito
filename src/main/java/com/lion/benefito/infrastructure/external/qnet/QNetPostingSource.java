package com.lion.benefito.infrastructure.external.qnet;

import com.lion.benefito.infrastructure.external.ExternalPostingData;
import com.lion.benefito.infrastructure.external.ExternalPostingSource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Q-Net "종목별 시험일정 목록 조회(getJMList)"는 jmCd(특정 종목) 없이는 호출할 수 없어(qualgbCd
 * 단독 조회 시 <items/> 빈 응답, 2026-07-24 실제 호출로 확인) 2단계로 동작한다:
 * (1) "국가자격 종목 목록 조회(getList)"로 전체 종목(jmCd + qualgbCd)을 받아
 * (2) 그중 국가자격 전체(T/C/W/S 네 구분)에 해당하는 jmCd만 추려 종목별로 getJMList를 호출한다.
 *
 * getJMList의 회차 1건(item)에는 필기/실기 일정이 함께 들어있지만, 실제 시험은 "필기시험"과
 * "실기시험"이 서로 다른 일정(원서접수/시험일/발표일)을 갖는 별개의 공고이므로 회차당 최대 2개의
 * Posting(필기/실기)으로 분리해 생성한다(사용자 요구사항: "2026년 정보처리기사 제1회 필기시험" /
 * "2026년 정보처리기사 제1회 실기시험"을 각각 별도 공고로).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QNetPostingSource implements ExternalPostingSource {

    private static final String SOURCE_TYPE = "QNET";
    private static final String FIXED_COMPANY = "한국산업인력공단(Q-Net)";
    private static final String HOMEPAGE_URL = "https://www.q-net.or.kr";
    // Q-Net 응답에는 지역 개념이 없어(전국 단위 시험) 지역 조건은 항상 "전국"으로 채운다.
    private static final String NATIONWIDE_REGION = "전국";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    // 모든 국가자격 시험 일정을 수집하기 위한 자격구분코드 전수(사용자 결정: 특정 종목 하나만이
    // 아니라 이 네 구분 전체를 대상으로 한다). 실제 데이터에는 T(국가기술자격)/S(국가전문자격)만
    // 존재하는 것으로 확인됐지만(2026-07-24), C/W도 향후 추가될 수 있어 필터에는 계속 남겨둔다.
    private static final List<String> QUALGB_CODES = List.of("T", "C", "W", "S");
    private static final Map<String, String> QUALGB_LABELS = Map.of(
            "T", "국가기술자격",
            "S", "국가전문자격",
            "C", "국가공인민간자격",
            "W", "외국자격");
    // getList는 numOfRows/pageNo를 무시하고 항상 전체 목록(2026-07-24 기준 613건)을 반환하지만,
    // 혹시 API 쪽에서 요청 건수만큼만 잘라 줄 가능성에 대비해 실제 건수보다 넉넉하게 요청한다.
    private static final int QUALIFICATION_LIST_ROWS = 5000;

    // 종목 600여 건을 지연 없이 연속 호출하면 Q-Net API가 resultCode=99("Failed to validate a newly
    // established connection")를 대량으로 반환한다(2026-07-24 실제 재수집으로 확인). data.go.kr 계열
    // 공공 API에서 흔한 "가용 세션 소진" 게이트웨이 오류와 동일한 패턴이다. 직접 실험한 결과
    // (2026-07-24) 요청 간격만으로는 잘 안정화되지 않았다(1초 간격 성공률 약 22%, 3초 간격 약 56%).
    // 반면 실패 시 짧게 대기 후 바로 재시도하는 쪽이 훨씬 효과적이었다(1.5초 백오프 + 최대 5회 재시도
    // 시 성공률 약 90%, 실패 자체가 200ms 내외로 빨리 응답해 재시도 비용이 낮다). 다만 한 번 세션
    // 풀이 오래 고갈되면(예: 지연 없이 수백 건을 연속 호출한 직후) 개별 재시도만으로는 회복되지 않는
    // 구간이 있어, 종목이 연속으로 여러 건 통째로 실패하면 더 길게 쉬었다 가는 서킷브레이커를 둔다.
    private static final long REQUEST_INTERVAL_MILLIS = 1000;
    private static final int MAX_ATTEMPTS = 5;
    private static final long INITIAL_RETRY_BACKOFF_MILLIS = 1500;
    // 재시도까지 다 써도 이 개수만큼 종목이 연속으로 실패하면 세션 풀이 고갈된 것으로 보고
    // COOLDOWN_MILLIS만큼 쉬었다가 계속한다.
    private static final int CONSECUTIVE_FAILURE_COOLDOWN_THRESHOLD = 3;
    private static final long COOLDOWN_MILLIS = 30_000;

    // implplannm(회차명) 표기가 종목마다 제각각("2026년 정기 기사 1회", "2026년도 1회",
    // "기술사(2026년도 제138회)", "상시" 등)이라 완벽한 파싱은 불가능하다. 아래 두 패턴은 title에만
    // 쓰이는 "표시용" 값을 뽑아내는 용도이고(파싱에 실패해도 원문 그대로 표시), 저장 식별자(externalId)는
    // implplannm 원문을 그대로 사용해 파싱 실패와 무관하게 항상 고유성이 보장된다.
    private static final Pattern YEAR_PATTERN = Pattern.compile("(\\d{4})년");
    private static final Pattern ROUND_PATTERN = Pattern.compile("(\\d+)\\s*회");

    private final QNetClient qNetClient;

    @Override
    public List<ExternalPostingData> fetchPostings() {
        Map<String, String> jmCdToQualgbCd = fetchTargetJmCds();
        if (jmCdToQualgbCd.isEmpty()) {
            log.warn("Q-Net 종목 목록에서 국가자격(T/C/W/S) 종목을 찾지 못했습니다.");
            return List.of();
        }

        List<ExternalPostingData> result = new ArrayList<>();
        int successCount = 0;
        int consecutiveFailures = 0;
        for (Map.Entry<String, String> entry : jmCdToQualgbCd.entrySet()) {
            sleepQuietly(REQUEST_INTERVAL_MILLIS);
            try {
                result.addAll(fetchScheduleAsPostingData(entry.getKey(), entry.getValue()));
                successCount++;
                consecutiveFailures = 0;
            } catch (Exception e) {
                log.warn("Q-Net 종목(jmCd={}) 시험일정 조회 실패: {}", entry.getKey(), e.getMessage());
                consecutiveFailures++;
                if (consecutiveFailures >= CONSECUTIVE_FAILURE_COOLDOWN_THRESHOLD) {
                    log.warn("Q-Net 종목이 {}건 연속으로 실패해 세션 풀 고갈로 보고 {}초 대기 후 계속합니다.",
                            consecutiveFailures, COOLDOWN_MILLIS / 1000);
                    sleepQuietly(COOLDOWN_MILLIS);
                    consecutiveFailures = 0;
                }
            }
        }
        log.info("Q-Net 종목 목록 {}건 중 {}건 시험일정 조회 성공, 시험일정(필기/실기 분리) {}건 수집.",
                jmCdToQualgbCd.size(), successCount, result.size());
        return result;
    }

    // 종목 목록 전체에서 국가자격(T/C/W/S) 종목의 jmCd -> qualgbCd만 중복 없이 추려낸다.
    private Map<String, String> fetchTargetJmCds() {
        QNetQualificationListResponse response;
        try {
            response = qNetClient.fetchQualificationList(QUALIFICATION_LIST_ROWS, 1);
        } catch (Exception e) {
            log.warn("Q-Net 종목 목록 조회 실패: {}", e.getMessage());
            return Map.of();
        }

        List<QNetQualificationItem> items = response.body() != null ? response.body().getItems() : null;
        if (items == null) {
            return Map.of();
        }
        if (items.size() >= QUALIFICATION_LIST_ROWS) {
            log.warn("Q-Net 종목 목록이 요청 건수({})만큼 꽉 차서 반환되어 일부가 누락됐을 수 있습니다.",
                    QUALIFICATION_LIST_ROWS);
        }

        Map<String, String> jmCdToQualgbCd = new LinkedHashMap<>();
        for (QNetQualificationItem item : items) {
            if (item.jmcd() == null || item.jmcd().isBlank() || !QUALGB_CODES.contains(item.qualgbcd())) {
                continue;
            }
            jmCdToQualgbCd.putIfAbsent(item.jmcd(), item.qualgbcd());
        }
        return jmCdToQualgbCd;
    }

    private List<ExternalPostingData> fetchScheduleAsPostingData(String jmCd, String qualgbCd) {
        QNetScheduleResponse response = fetchScheduleWithRetry(jmCd);
        List<QNetScheduleItem> items = response.body() != null ? response.body().getItems() : null;
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<ExternalPostingData> result = new ArrayList<>();
        for (QNetScheduleItem item : items) {
            if (item.implplannm() == null || item.implplannm().isBlank()) {
                log.warn("Q-Net 일정 파싱 실패(jmCd={}): 시험회차명(implplannm)이 없습니다.", jmCd);
                continue;
            }
            addIfPresent(result, jmCd, qualgbCd, item, ExamType.WRITTEN);
            addIfPresent(result, jmCd, qualgbCd, item, ExamType.PRACTICAL);
        }
        return result;
    }

    // resultCode=99("Failed to validate a newly established connection")는 일시적 연결 문제로 보여
    // 짧은 대기 후 재시도하면 성공하는 경우가 많다(2026-07-24 확인). 마지막 시도까지 실패하면 그 예외를
    // 그대로 던져 호출부(fetchPostings)가 해당 종목만 건너뛰도록 한다.
    private QNetScheduleResponse fetchScheduleWithRetry(String jmCd) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return qNetClient.fetchSchedule(jmCd);
            } catch (RuntimeException e) {
                lastError = e;
                if (attempt < MAX_ATTEMPTS) {
                    // 지수 백오프: 1.5초, 3초, 6초, 12초 순으로 늘려가며 재시도한다.
                    sleepQuietly(INITIAL_RETRY_BACKOFF_MILLIS * (1L << (attempt - 1)));
                }
            }
        }
        throw lastError;
    }

    // 테스트에서 재시도/서킷브레이커 횟수만 검증하고 실제 대기(최악의 경우 개별 종목당 초 단위,
    // 서킷브레이커까지 겹치면 수십 초)는 걷어내기 위한 훅. 운영에서는 항상 실제로 대기한다.
    private LongConsumer sleeper = this::realSleep;

    private void sleepQuietly(long millis) {
        sleeper.accept(millis);
    }

    private void realSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    void setSleeperForTest(LongConsumer sleeper) {
        this.sleeper = sleeper;
    }

    private void addIfPresent(List<ExternalPostingData> result, String jmCd, String qualgbCd,
                               QNetScheduleItem item, ExamType examType) {
        try {
            ExternalPostingData data = toExternalPostingData(jmCd, qualgbCd, item, examType);
            if (data != null) {
                result.add(data);
            }
        } catch (Exception e) {
            log.warn("Q-Net 일정 파싱 실패(jmCd={}, implplannm={}, 구분={}): {}",
                    jmCd, item.implplannm(), examType.label, e.getMessage());
        }
    }

    // 이 회차에 해당 구분(필기/실기) 시험 자체가 없으면(예: 실기가 없는 종목) null을 반환해 건너뛴다.
    private ExternalPostingData toExternalPostingData(String jmCd, String qualgbCd,
                                                        QNetScheduleItem item, ExamType examType) {
        String regStart = examType.regStart(item);
        String regEnd = examType.regEnd(item);
        String examStart = examType.examStart(item);
        String examEnd = examType.examEnd(item);

        if (isBlank(regStart) && isBlank(regEnd) && isBlank(examStart) && isBlank(examEnd)) {
            return null;
        }

        LocalDate deadline = resolveDeadline(jmCd, item, examType, regEnd, examEnd, examStart, regStart);
        if (deadline == null) {
            return null;
        }
        LocalDate startDate = !isBlank(regStart) ? parseDate(regStart) : null;

        // jmCd + 회차 원문(implplannm) + 구분(필기/실기)으로 유일성을 보장한다(같은 종목·회차 재수집 시
        // upsert, 다른 종목/회차/구분과는 절대 겹치지 않음 -> ExternalPostingIngestService가 이 값으로
        // 중복 없이 저장). implplannm 자체에 시행연도와 회차가 함께 담겨 있어 별도로 분리하지 않는다.
        String externalId = jmCd + "-" + item.implplannm() + "-" + examType.label;
        String title = buildTitle(item.jmfldnm(), item.implplannm(), examType);

        return new ExternalPostingData(
                SOURCE_TYPE,
                externalId,
                title,
                buildContent(qualgbCd, item, examType),
                FIXED_COMPANY,
                HOMEPAGE_URL,
                null,
                NATIONWIDE_REGION,
                buildQualification(item),
                startDate,
                deadline);
    }

    // 원서접수 마감일이 없으면(원서접수 기간 정보 자체가 없는 종목) 시험일을 마감일 대체로 사용한다
    // (사용자 결정: 항목을 통째로 버리는 대신 시험일로 대체하되, 대체 발생 시 항상 로그로 남겨 보고에 반영한다).
    private LocalDate resolveDeadline(String jmCd, QNetScheduleItem item, ExamType examType,
                                       String regEnd, String examEnd, String examStart, String regStart) {
        if (!isBlank(regEnd)) {
            return parseDate(regEnd);
        }
        String fallback = !isBlank(examEnd) ? examEnd : (!isBlank(examStart) ? examStart : regStart);
        if (isBlank(fallback)) {
            return null;
        }
        log.warn("Q-Net 종목(jmCd={}, 시험회차={}, 구분={}) 원서접수 마감일 정보가 없어 시험일({})을 마감일로 대체합니다.",
                jmCd, item.implplannm(), examType.label, fallback);
        return parseDate(fallback);
    }

    private String buildTitle(String jmfldnm, String implplannm, ExamType examType) {
        String year = extractYear(implplannm);
        String round = extractRound(implplannm);
        StringBuilder sb = new StringBuilder();
        if (!year.isBlank()) {
            sb.append(year).append("년 ");
        }
        sb.append(jmfldnm).append(' ').append(round).append(' ').append(examType.label).append("시험");
        return sb.toString().strip();
    }

    private String extractYear(String implplannm) {
        Matcher matcher = YEAR_PATTERN.matcher(implplannm);
        return matcher.find() ? matcher.group(1) : "";
    }

    // "제N회" 형태로 정규화한다. 회차 숫자를 찾지 못하면(예: "상시") implplannm 원문을 그대로 사용한다.
    private String extractRound(String implplannm) {
        Matcher matcher = ROUND_PATTERN.matcher(implplannm);
        return matcher.find() ? "제" + matcher.group(1) + "회" : implplannm.strip();
    }

    // 종목명, 자격구분, 시험회차, 필기/실기 구분, 해당 구분의 원서접수 기간/시험일/합격자 발표일을 담는다.
    private String buildContent(String qualgbCd, QNetScheduleItem item, ExamType examType) {
        StringBuilder sb = new StringBuilder();
        sb.append("종목명: ").append(item.jmfldnm()).append('\n');
        sb.append("자격구분: ").append(QUALGB_LABELS.getOrDefault(qualgbCd, qualgbCd)).append('\n');
        sb.append("시험회차: ").append(item.implplannm()).append('\n');
        sb.append("구분: ").append(examType.label).append("시험\n");
        appendRange(sb, "원서접수", examType.regStart(item), examType.regEnd(item));
        appendRange(sb, "시험일", examType.examStart(item), examType.examEnd(item));
        if (examType == ExamType.WRITTEN) {
            appendDate(sb, "합격자 발표", item.docPassDt());
        } else {
            appendRange(sb, "합격자 발표", item.pracPassStartDt(), item.pracPassEndDt());
        }
        return sb.toString().strip();
    }

    private void appendRange(StringBuilder sb, String label, String start, String end) {
        if (isBlank(start)) {
            return;
        }
        String range = (!isBlank(end) && !end.equals(start)) ? start + " ~ " + end : start;
        sb.append(label).append(": ").append(range).append('\n');
    }

    private void appendDate(StringBuilder sb, String label, String date) {
        if (isBlank(date)) {
            return;
        }
        sb.append(label).append(": ").append(date).append('\n');
    }

    // Q-Net 응답엔 지원자격 텍스트가 없어, 가장 가까운 대안으로 대/중직무분야를 사용한다.
    // 종목명(jmfldnm)은 항상 값이 있으므로 "분야" 태그로 맨 앞에 노출해 분야 정보가 비어 보이지 않게 한다.
    private String buildQualification(QNetScheduleItem item) {
        String fieldTag = "분야: " + item.jmfldnm();
        String jobField = buildJobField(item.obligfldnm(), item.mdobligfldnm());
        return jobField == null ? fieldTag : fieldTag + "\n" + jobField;
    }

    private String buildJobField(String major, String middle) {
        if (isBlank(major)) {
            return middle;
        }
        if (isBlank(middle)) {
            return major;
        }
        return major + " > " + middle;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // 실제 응답 확인 결과 각 날짜 필드는 "20260115"와 같은 yyyyMMdd 8자리 숫자 문자열이다.
    // 형식이 잘못돼 있으면 해당 구분(필기 또는 실기) 항목만 제외하고 계속 진행한다(호출부 addIfPresent에서 catch).
    private LocalDate parseDate(String rawDate) {
        try {
            return LocalDate.parse(rawDate, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다: " + rawDate, e);
        }
    }

    private enum ExamType {
        WRITTEN("필기") {
            String regStart(QNetScheduleItem item) {
                return item.docRegStartDt();
            }

            String regEnd(QNetScheduleItem item) {
                return item.docRegEndDt();
            }

            String examStart(QNetScheduleItem item) {
                return item.docExamStartDt();
            }

            String examEnd(QNetScheduleItem item) {
                return item.docExamEndDt();
            }
        },
        PRACTICAL("실기") {
            String regStart(QNetScheduleItem item) {
                return item.pracRegStartDt();
            }

            String regEnd(QNetScheduleItem item) {
                return item.pracRegEndDt();
            }

            String examStart(QNetScheduleItem item) {
                return item.pracExamStartDt();
            }

            String examEnd(QNetScheduleItem item) {
                return item.pracExamEndDt();
            }
        };

        final String label;

        ExamType(String label) {
            this.label = label;
        }

        abstract String regStart(QNetScheduleItem item);

        abstract String regEnd(QNetScheduleItem item);

        abstract String examStart(QNetScheduleItem item);

        abstract String examEnd(QNetScheduleItem item);
    }
}
