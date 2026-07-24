package com.lion.benefito.infrastructure.external.seed;

import com.lion.benefito.infrastructure.external.ExternalPostingData;
import com.lion.benefito.infrastructure.external.ExternalPostingSource;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Q-Net 실시간 수집(QNetPostingSource)이 외부 API 오류로 아직 안정적으로 동작하지 않는 동안,
 * 프론트/챗봇 테스트용으로 실제 자격증 시험 공고 형태를 갖춘 임시 시드 데이터를 제공한다.
 *
 * sourceType을 QNET과 분리된 "SEED_QNET_SCHEDULE"로 고정해 실제 Q-Net 수집 데이터와 절대 섞이지
 * 않게 한다 - 나중에 Q-Net 수집이 정상화되면 `DELETE FROM postings WHERE source_type =
 * 'SEED_QNET_SCHEDULE'` 한 줄로 이 시드만 안전하게 제거할 수 있다(QNET 소스는 건드리지 않음).
 *
 * externalId는 "자격증명-2026-회차-시험구분" 형태로 고정해 (sourceType, externalId) 유니크 제약과
 * 결합하면 자격증명+시행연도+회차+시험구분 조합이 곧 고유키가 된다. ExternalPostingIngestService의
 * 기존 upsert 파이프라인(findBySourceTypeAndExternalId)을 그대로 타므로, 애플리케이션을 재시작하고
 * 이 소스를 다시 수집해도 새 행이 생기지 않고 UNCHANGED로 처리된다.
 */
@Component
public class SeedQNetScheduleSource implements ExternalPostingSource {

    private static final String SOURCE_TYPE = "SEED_QNET_SCHEDULE";
    private static final String YEAR = "2026";
    private static final String NATIONWIDE_REGION = "전국";

    private static final String QNET_ORG = "한국산업인력공단(Q-Net)";
    private static final String QNET_URL = "https://www.q-net.or.kr";
    private static final String KORCHAM_ORG = "대한상공회의소";
    private static final String KORCHAM_URL = "https://license.korcham.net";
    private static final String HISTORY_ORG = "국사편찬위원회";
    private static final String HISTORY_URL = "https://www.historyexam.go.kr";
    private static final String DATAQ_ORG = "한국데이터산업진흥원";
    private static final String DATAQ_URL = "https://www.dataq.or.kr";

    // 실제 Q-Net 시험 공고와 동일한 구성(원서접수 시작/마감, 시험일, 합격자 발표일)을 갖춘 시드 26건.
    // 모든 원서접수 마감일(deadline)을 오늘(2026-07-24) 이후로 잡아 PostingMapper.resolveStatus가
    // 전부 ACTIVE로 판정하게 한다(요구사항: status=ACTIVE).
    private static final List<ExamSeed> SEEDS = List.of(
            new ExamSeed("2026년 정보처리기사 제3회 필기시험", "정보처리기사", "3회", "필기",
                    LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 7),
                    LocalDate.of(2026, 8, 22), LocalDate.of(2026, 9, 9), QNET_ORG, QNET_URL),
            new ExamSeed("2026년 정보처리기사 제3회 실기시험", "정보처리기사", "3회", "실기",
                    LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 18),
                    LocalDate.of(2026, 10, 17), LocalDate.of(2026, 11, 18), QNET_ORG, QNET_URL),
            new ExamSeed("2026년 전기기사 제3회 필기시험", "전기기사", "3회", "필기",
                    LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 7),
                    LocalDate.of(2026, 8, 23), LocalDate.of(2026, 9, 9), QNET_ORG, QNET_URL),
            new ExamSeed("2026년 전기기사 제3회 실기시험", "전기기사", "3회", "실기",
                    LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 18),
                    LocalDate.of(2026, 10, 18), LocalDate.of(2026, 11, 18), QNET_ORG, QNET_URL),
            new ExamSeed("2026년 전기기사 제4회 필기시험", "전기기사", "4회", "필기",
                    LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 16),
                    LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 18), QNET_ORG, QNET_URL),
            new ExamSeed("2026년 전기기사 제4회 실기시험", "전기기사", "4회", "실기",
                    LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 27),
                    LocalDate.of(2026, 12, 19), LocalDate.of(2027, 1, 13), QNET_ORG, QNET_URL),
            new ExamSeed("2026년 산업안전기사 제3회 필기시험", "산업안전기사", "3회", "필기",
                    LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 21),
                    LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 23), QNET_ORG, QNET_URL),
            new ExamSeed("2026년 산업안전기사 제2회 실기시험", "산업안전기사", "2회", "실기",
                    LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 2),
                    LocalDate.of(2026, 11, 8), LocalDate.of(2026, 12, 9), QNET_ORG, QNET_URL),
            new ExamSeed("2026년 건축기사 제2회 필기시험", "건축기사", "2회", "필기",
                    LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 21),
                    LocalDate.of(2026, 9, 6), LocalDate.of(2026, 9, 23), QNET_ORG, QNET_URL),
            new ExamSeed("2026년 건축기사 제3회 실기시험", "건축기사", "3회", "실기",
                    LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 2),
                    LocalDate.of(2026, 11, 7), LocalDate.of(2026, 12, 9), QNET_ORG, QNET_URL),
            new ExamSeed("2026년 소방설비기사(전기분야) 제3회 필기시험", "소방설비기사(전기분야)", "3회", "필기",
                    LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 21),
                    LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 23), QNET_ORG, QNET_URL),
            new ExamSeed("2026년 소방설비기사(전기분야) 제3회 실기시험", "소방설비기사(전기분야)", "3회", "실기",
                    LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 2),
                    LocalDate.of(2026, 11, 8), LocalDate.of(2026, 12, 9), QNET_ORG, QNET_URL),
            new ExamSeed("2026년 한식조리기능사 상시 필기시험", "한식조리기능사", "상시", "필기",
                    LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 14),
                    LocalDate.of(2026, 8, 29), LocalDate.of(2026, 9, 4), QNET_ORG, QNET_URL),
            new ExamSeed("2026년 한식조리기능사 상시 실기시험", "한식조리기능사", "상시", "실기",
                    LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 11),
                    LocalDate.of(2026, 9, 27), LocalDate.of(2026, 10, 16), QNET_ORG, QNET_URL),
            new ExamSeed("2026년 빅데이터분석기사 제3회 필기시험", "빅데이터분석기사", "3회", "필기",
                    LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 28),
                    LocalDate.of(2026, 9, 19), LocalDate.of(2026, 10, 7), QNET_ORG, QNET_URL),
            new ExamSeed("2026년 빅데이터분석기사 제3회 실기시험", "빅데이터분석기사", "3회", "실기",
                    LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 23),
                    LocalDate.of(2026, 11, 21), LocalDate.of(2026, 12, 9), QNET_ORG, QNET_URL),
            new ExamSeed("2026년 정보보안기사 제3회 필기시험", "정보보안기사", "3회", "필기",
                    LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 4),
                    LocalDate.of(2026, 9, 26), LocalDate.of(2026, 10, 14), QNET_ORG, QNET_URL),
            new ExamSeed("2026년 정보보안기사 제3회 실기시험", "정보보안기사", "3회", "실기",
                    LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 16),
                    LocalDate.of(2026, 11, 14), LocalDate.of(2026, 12, 16), QNET_ORG, QNET_URL),
            new ExamSeed("2026년 컴퓨터활용능력 1급 필기시험", "컴퓨터활용능력 1급", "5회", "필기",
                    LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 7),
                    LocalDate.of(2026, 8, 22), LocalDate.of(2026, 8, 28), KORCHAM_ORG, KORCHAM_URL),
            new ExamSeed("2026년 컴퓨터활용능력 1급 실기시험", "컴퓨터활용능력 1급", "5회", "실기",
                    LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 7),
                    LocalDate.of(2026, 9, 19), LocalDate.of(2026, 9, 25), KORCHAM_ORG, KORCHAM_URL),
            new ExamSeed("2026년 한국사능력검정시험 심화(제76회)", "한국사능력검정시험 심화", "76회", "심화",
                    LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 10),
                    LocalDate.of(2026, 8, 29), LocalDate.of(2026, 9, 11), HISTORY_ORG, HISTORY_URL),
            new ExamSeed("2026년 한국사능력검정시험 심화(제77회)", "한국사능력검정시험 심화", "77회", "심화",
                    LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12),
                    LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 13), HISTORY_ORG, HISTORY_URL),
            new ExamSeed("2026년 SQLD 제1회 시험", "SQLD", "1회", "전체",
                    LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 21),
                    LocalDate.of(2026, 9, 19), LocalDate.of(2026, 10, 8), DATAQ_ORG, DATAQ_URL),
            new ExamSeed("2026년 SQLD 제2회 시험", "SQLD", "2회", "전체",
                    LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 20),
                    LocalDate.of(2026, 12, 19), LocalDate.of(2027, 1, 7), DATAQ_ORG, DATAQ_URL),
            new ExamSeed("2026년 ADsP 제1회 시험", "ADsP", "1회", "전체",
                    LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 21),
                    LocalDate.of(2026, 9, 19), LocalDate.of(2026, 10, 8), DATAQ_ORG, DATAQ_URL),
            new ExamSeed("2026년 ADsP 제2회 시험", "ADsP", "2회", "전체",
                    LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 20),
                    LocalDate.of(2026, 12, 19), LocalDate.of(2027, 1, 7), DATAQ_ORG, DATAQ_URL)
    );

    @Override
    public List<ExternalPostingData> fetchPostings() {
        return SEEDS.stream().map(this::toExternalPostingData).toList();
    }

    private ExternalPostingData toExternalPostingData(ExamSeed seed) {
        String externalId = "%s-%s-%s-%s".formatted(seed.qualificationName, YEAR, seed.round, seed.examType);

        return new ExternalPostingData(
                SOURCE_TYPE,
                externalId,
                seed.title,
                buildContent(seed),
                seed.organization,
                seed.homepageUrl,
                null,
                NATIONWIDE_REGION,
                seed.examType,
                seed.regStart,
                seed.regEnd);
    }

    private String buildContent(ExamSeed seed) {
        return """
                종목명: %s
                시험회차: %s
                구분: %s
                원서접수: %s ~ %s
                시험일: %s
                합격자 발표일: %s"""
                .formatted(seed.qualificationName, seed.round, seed.examType,
                        seed.regStart, seed.regEnd, seed.examDate, seed.announceDate);
    }

    private record ExamSeed(
            String title,
            String qualificationName,
            String round,
            String examType,
            LocalDate regStart,
            LocalDate regEnd,
            LocalDate examDate,
            LocalDate announceDate,
            String organization,
            String homepageUrl
    ) {
    }
}
