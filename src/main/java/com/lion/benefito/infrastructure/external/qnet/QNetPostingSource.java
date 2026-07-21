package com.lion.benefito.infrastructure.external.qnet;

import com.lion.benefito.infrastructure.external.ExternalPostingData;
import com.lion.benefito.infrastructure.external.ExternalPostingErrorCode;
import com.lion.benefito.infrastructure.external.ExternalPostingException;
import com.lion.benefito.infrastructure.external.ExternalPostingSource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Q-Net에는 "종목별 시험일정 목록 조회(jmCd 필수)"만 있고 jmCd 자체를 나열해주는 API는 없으므로,
 * (1) 종목 목록 API로 jmCd/종목명을 조회 -> (2) target-codes에 해당하는 jmCd만 골라 -> (3) 각각
 * getJMList를 호출해 ExternalPostingData로 변환하는 2단계 흐름으로 동작한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QNetPostingSource implements ExternalPostingSource {

    private static final String SOURCE_TYPE = "QNET";
    private static final String FIXED_COMPANY = "한국산업인력공단";
    private static final String HOMEPAGE_URL = "https://www.q-net.or.kr";
    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int QUALIFICATION_LIST_ROWS = 700;

    private final QNetClient qNetClient;
    private final QNetProperties properties;

    @Override
    public List<ExternalPostingData> fetchPostings() {
        Map<String, String> targetQualifications = fetchTargetQualifications();
        if (targetQualifications.isEmpty()) {
            log.warn("external.qnet.target-codes에 해당하는 종목을 찾지 못했습니다.");
            return List.of();
        }

        List<ExternalPostingData> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : targetQualifications.entrySet()) {
            String jmCd = entry.getKey();
            try {
                result.addAll(fetchScheduleAsPostingData(jmCd));
            } catch (Exception e) {
                log.warn("Q-Net 종목(jmCd={}) 시험일정 조회 실패: {}", jmCd, e.getMessage());
            }
        }
        return result;
    }

    // 종목 목록에서 target-codes에 해당하는 jmCd -> 종목명만 추려낸다.
    private Map<String, String> fetchTargetQualifications() {
        List<String> targetCodes = properties.getTargetCodes();
        if (targetCodes == null || targetCodes.isEmpty()) {
            return Map.of();
        }

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

        Map<String, String> matched = new LinkedHashMap<>();
        for (QNetQualificationItem item : items) {
            if (targetCodes.contains(item.jmcd())) {
                matched.put(item.jmcd(), item.jmfldnm());
            }
        }
        return matched;
    }

    private List<ExternalPostingData> fetchScheduleAsPostingData(String jmCd) {
        QNetScheduleResponse response = qNetClient.fetchSchedule(jmCd);
        List<QNetScheduleItem> items = response.body() != null ? response.body().getItems() : null;
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<ExternalPostingData> result = new ArrayList<>();
        for (QNetScheduleItem item : items) {
            try {
                result.add(toExternalPostingData(jmCd, item));
            } catch (Exception e) {
                log.warn("Q-Net 일정 파싱 실패(jmCd={}, implplannm={}): {}", jmCd, item.implplannm(), e.getMessage());
            }
        }
        return result;
    }

    private ExternalPostingData toExternalPostingData(String jmCd, QNetScheduleItem item) {
        if (item.implplannm() == null || item.implplannm().isBlank()) {
            throw new ExternalPostingException(ExternalPostingErrorCode.INVALID_RESPONSE);
        }

        String externalId = jmCd + "-" + item.implplannm();
        String title = item.jmfldnm() + " " + item.implplannm();

        return new ExternalPostingData(
                SOURCE_TYPE,
                externalId,
                title,
                buildContent(item),
                FIXED_COMPANY,
                HOMEPAGE_URL,
                null,
                null,
                buildQualification(item),
                null,
                parseDeadline(item.docRegEndDt())
        );
    }

    // Q-Net 응답에는 별도의 "공고 내용" 텍스트가 없어, 접수/시험/발표 일정 자체를 content로 구성한다.
    private String buildContent(QNetScheduleItem item) {
        StringBuilder sb = new StringBuilder();
        appendRange(sb, "필기시험 원서접수", item.docRegStartDt(), item.docRegEndDt());
        appendRange(sb, "필기시험 일자", item.docExamStartDt(), item.docExamEndDt());
        appendDate(sb, "필기시험 합격자 발표", item.docPassDt());
        appendRange(sb, "실기시험 원서접수", item.pracRegStartDt(), item.pracRegEndDt());
        appendRange(sb, "실기시험 일자", item.pracExamStartDt(), item.pracExamEndDt());
        appendRange(sb, "합격자 발표", item.pracPassStartDt(), item.pracPassEndDt());
        return sb.toString().strip();
    }

    private void appendRange(StringBuilder sb, String label, String start, String end) {
        if (start == null || start.isBlank()) {
            return;
        }
        String range = (end != null && !end.isBlank() && !end.equals(start)) ? start + " ~ " + end : start;
        sb.append(label).append(": ").append(range).append("\n");
    }

    private void appendDate(StringBuilder sb, String label, String date) {
        if (date == null || date.isBlank()) {
            return;
        }
        sb.append(label).append(": ").append(date).append("\n");
    }

    // Q-Net 응답엔 지원자격 텍스트가 없어, 가장 가까운 대안으로 대/중직무분야를 사용한다.
    private String buildQualification(QNetScheduleItem item) {
        String major = item.obligfldnm();
        String middle = item.mdobligfldnm();
        if (major == null || major.isBlank()) {
            return middle;
        }
        if (middle == null || middle.isBlank()) {
            return major;
        }
        return major + " > " + middle;
    }

    // 실제 응답 확인 결과 docregenddt는 "20260115"와 같은 yyyyMMdd 8자리 숫자 문자열이다(다른 소스와 동일 포맷).
    private LocalDate parseDeadline(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            throw new ExternalPostingException(ExternalPostingErrorCode.INVALID_RESPONSE);
        }
        try {
            return LocalDate.parse(rawDate, DEADLINE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new ExternalPostingException(ExternalPostingErrorCode.INVALID_RESPONSE);
        }
    }
}
