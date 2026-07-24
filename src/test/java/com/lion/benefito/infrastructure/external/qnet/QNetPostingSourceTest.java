package com.lion.benefito.infrastructure.external.qnet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lion.benefito.infrastructure.external.ExternalPostingData;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QNetPostingSourceTest {

    private QNetClient client;
    private QNetPostingSource source;

    @BeforeEach
    void setUp() {
        client = mock(QNetClient.class);
        source = new QNetPostingSource(client);
        // 요청 간격/재시도 백오프/서킷브레이커 대기를 실제로 하지 않도록 걷어낸다(횟수/분기만 검증).
        source.setSleeperForTest(millis -> { });
    }

    private QNetQualificationListResponse qualificationList(QNetQualificationItem... items) {
        return new QNetQualificationListResponse(
                new QNetResponseHeader("00", "NORMAL SERVICE."),
                new QNetQualificationListResponse.Body(List.of(items)));
    }

    private QNetScheduleResponse scheduleResponse(QNetScheduleItem... items) {
        return new QNetScheduleResponse(
                new QNetResponseHeader("00", "NORMAL SERVICE."),
                new QNetScheduleResponse.Body(List.of(items)));
    }

    // 필기/실기 일정이 모두 있는 표준 회차.
    private QNetScheduleItem fullScheduleItem(String implplannm) {
        return new QNetScheduleItem("정보처리기사", implplannm,
                "20260112", "20260115", "20260130", "20260303", "20260311",
                "20260323", "20260326", "20260418", "20260506", "20260612", "20260811",
                "정보통신", "정보기술");
    }

    private QNetScheduleItem writtenOnlyScheduleItem(String implplannm) {
        return new QNetScheduleItem("한식조리기능사", implplannm,
                "20260112", "20260115", "20260130", "20260303", "20260311",
                null, null, null, null, null, null,
                "음식서비스", "조리");
    }

    private Optional<ExternalPostingData> written(List<ExternalPostingData> result) {
        return result.stream().filter(d -> d.title().contains("필기시험")).findFirst();
    }

    private Optional<ExternalPostingData> practical(List<ExternalPostingData> result) {
        return result.stream().filter(d -> d.title().contains("실기시험")).findFirst();
    }

    @Test
    void 종목_목록에서_T_C_W_S_자격구분에_해당하는_종목만_시험일정을_조회한다() {
        when(client.fetchQualificationList(5000, 1)).thenReturn(qualificationList(
                new QNetQualificationItem("1320", "정보처리기사", "T"),
                new QNetQualificationItem("5210", "공인노무사", "S"),
                new QNetQualificationItem("9999", "미분류종목", "X")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(fullScheduleItem("2026년 정기 기사 1회")));
        when(client.fetchSchedule("5210")).thenReturn(scheduleResponse(fullScheduleItem("2026년도 1회")));

        List<ExternalPostingData> result = source.fetchPostings();

        // 종목 2개 x (필기+실기) = 4건
        assertThat(result).hasSize(4);
        verify(client, never()).fetchSchedule("9999");
    }

    @Test
    void 종목_목록이_비어있으면_아무것도_조회하지_않는다() {
        when(client.fetchQualificationList(5000, 1)).thenReturn(qualificationList());

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).isEmpty();
        verify(client, never()).fetchSchedule(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void 종목_목록_조회_자체가_실패하면_빈_목록을_반환한다() {
        when(client.fetchQualificationList(5000, 1)).thenThrow(new RuntimeException("API 오류"));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).isEmpty();
        verify(client, never()).fetchSchedule(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void 같은_jmcd가_목록에_중복으로_있어도_한_번만_조회한다() {
        when(client.fetchQualificationList(5000, 1)).thenReturn(qualificationList(
                new QNetQualificationItem("1320", "정보처리기사", "T"),
                new QNetQualificationItem("1320", "정보처리기사", "T")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(fullScheduleItem("2026년 정기 기사 1회")));

        source.fetchPostings();

        verify(client, org.mockito.Mockito.times(1)).fetchSchedule("1320");
    }

    @Test
    void 종목별_다중_시험회차는_모두_별도_공고로_수집된다() {
        when(client.fetchQualificationList(5000, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("1320", "정보처리기사", "T")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(
                fullScheduleItem("2026년 정기 기사 1회"),
                fullScheduleItem("2026년 정기 기사 2회"),
                fullScheduleItem("2026년 정기 기사 3회")));

        List<ExternalPostingData> result = source.fetchPostings();

        // 회차 3개 x (필기+실기) = 6건, externalId는 전부 서로 달라야 한다.
        assertThat(result).hasSize(6);
        assertThat(result.stream().map(ExternalPostingData::externalId).distinct()).hasSize(6);
        assertThat(result.stream().map(ExternalPostingData::title).distinct()).containsExactlyInAnyOrder(
                "2026년 정보처리기사 제1회 필기시험", "2026년 정보처리기사 제1회 실기시험",
                "2026년 정보처리기사 제2회 필기시험", "2026년 정보처리기사 제2회 실기시험",
                "2026년 정보처리기사 제3회 필기시험", "2026년 정보처리기사 제3회 실기시험");
    }

    @Test
    void 같은_회차를_재수집해도_동일한_externalId가_생성되어_중복_저장을_방지한다() {
        when(client.fetchQualificationList(5000, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("1320", "정보처리기사", "T")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(fullScheduleItem("2026년 정기 기사 1회")));

        List<ExternalPostingData> first = source.fetchPostings();
        List<ExternalPostingData> second = source.fetchPostings();

        assertThat(first.stream().map(ExternalPostingData::externalId).sorted().toList())
                .isEqualTo(second.stream().map(ExternalPostingData::externalId).sorted().toList());
    }

    @Test
    void 한_회차는_필기_실기_각각_별도_공고로_분리된다() {
        when(client.fetchQualificationList(5000, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("1320", "정보처리기사", "T")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(fullScheduleItem("2026년 정기 기사 1회")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).hasSize(2);
        assertThat(written(result)).isPresent();
        assertThat(practical(result)).isPresent();
    }

    @Test
    void 실기_일정이_없으면_필기_공고만_생성된다() {
        when(client.fetchQualificationList(5000, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("6805", "한식조리기능사", "T")));
        when(client.fetchSchedule("6805")).thenReturn(scheduleResponse(writtenOnlyScheduleItem("상시")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).contains("필기시험");
    }

    @Test
    void sourceType과_externalId는_jmcd_회차원문_구분을_조합한다() {
        when(client.fetchQualificationList(5000, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("1320", "정보처리기사", "T")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(fullScheduleItem("2026년 정기 기사 1회")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(written(result)).get().satisfies(d -> {
            assertThat(d.sourceType()).isEqualTo("QNET");
            assertThat(d.externalId()).isEqualTo("1320-2026년 정기 기사 1회-필기");
        });
        assertThat(practical(result)).get().satisfies(d ->
                assertThat(d.externalId()).isEqualTo("1320-2026년 정기 기사 1회-실기"));
    }

    @Test
    void title은_시행연도_종목명_회차_시험구분을_조합한다() {
        when(client.fetchQualificationList(5000, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("1320", "정보처리기사", "T")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(fullScheduleItem("2026년 정기 기사 1회")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(written(result)).get()
                .extracting(ExternalPostingData::title)
                .isEqualTo("2026년 정보처리기사 제1회 필기시험");
        assertThat(practical(result)).get()
                .extracting(ExternalPostingData::title)
                .isEqualTo("2026년 정보처리기사 제1회 실기시험");
    }

    @Test
    void 회차_표기에_회차_숫자가_없으면_원문을_그대로_회차로_사용한다() {
        when(client.fetchQualificationList(5000, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("6805", "한식조리기능사", "T")));
        when(client.fetchSchedule("6805")).thenReturn(scheduleResponse(writtenOnlyScheduleItem("상시")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).title()).isEqualTo("한식조리기능사 상시 필기시험");
    }

    @Test
    void organization은_한국산업인력공단_QNET로_고정된다() {
        when(client.fetchQualificationList(5000, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("1320", "정보처리기사", "T")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(fullScheduleItem("2026년 정기 기사 1회")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).organization()).isEqualTo("한국산업인력공단(Q-Net)");
    }

    @Test
    void deadline은_필기와_실기_각각_원서접수_종료일로_yyyyMMdd_파싱한다() {
        when(client.fetchQualificationList(5000, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("1320", "정보처리기사", "T")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(fullScheduleItem("2026년 정기 기사 1회")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(written(result)).get().extracting(ExternalPostingData::deadline)
                .isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(practical(result)).get().extracting(ExternalPostingData::deadline)
                .isEqualTo(LocalDate.of(2026, 3, 26));
    }

    @Test
    void startDate는_필기와_실기_각각_원서접수_시작일이다() {
        when(client.fetchQualificationList(5000, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("1320", "정보처리기사", "T")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(fullScheduleItem("2026년 정기 기사 1회")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(written(result)).get().extracting(ExternalPostingData::startDate)
                .isEqualTo(LocalDate.of(2026, 1, 12));
        assertThat(practical(result)).get().extracting(ExternalPostingData::startDate)
                .isEqualTo(LocalDate.of(2026, 3, 23));
    }

    @Test
    void 원서접수_종료일이_없으면_시험일을_마감일로_대체한다() {
        QNetScheduleItem noRegPeriod = new QNetScheduleItem("정보처리기사", "2026년 정기 기사 1회",
                null, null, "20260130", "20260303", "20260311",
                null, null, null, null, null, null,
                "정보통신", "정보기술");
        when(client.fetchQualificationList(5000, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("1320", "정보처리기사", "T")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(noRegPeriod));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).deadline()).isEqualTo(LocalDate.of(2026, 3, 3));
    }

    @Test
    void region은_항상_전국으로_채운다() {
        when(client.fetchQualificationList(5000, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("1320", "정보처리기사", "T")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(fullScheduleItem("2026년 정기 기사 1회")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).allSatisfy(d -> assertThat(d.region()).isEqualTo("전국"));
    }

    @Test
    void content는_해당_구분의_일정만_사람이_읽기_좋은_형태로_담는다() {
        when(client.fetchQualificationList(5000, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("1320", "정보처리기사", "T")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(fullScheduleItem("2026년 정기 기사 1회")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(written(result)).get().extracting(ExternalPostingData::content).satisfies(content -> {
            assertThat((String) content).contains("종목명: 정보처리기사");
            assertThat((String) content).contains("자격구분: 국가기술자격");
            assertThat((String) content).contains("시험회차: 2026년 정기 기사 1회");
            assertThat((String) content).contains("구분: 필기시험");
            assertThat((String) content).contains("원서접수: 20260112 ~ 20260115");
            assertThat((String) content).contains("합격자 발표: 20260311");
            assertThat((String) content).doesNotContain("구분: 실기시험");
        });
        assertThat(practical(result)).get().extracting(ExternalPostingData::content).satisfies(content -> {
            assertThat((String) content).contains("구분: 실기시험");
            assertThat((String) content).contains("합격자 발표: 20260612 ~ 20260811");
        });
    }

    @Test
    void qualification은_분야_태그와_대직무분야_중직무분야_조합을_함께_담는다() {
        when(client.fetchQualificationList(5000, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("1320", "정보처리기사", "T")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(fullScheduleItem("2026년 정기 기사 1회")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).qualification()).isEqualTo("분야: 정보처리기사\n정보통신 > 정보기술");
    }

    @Test
    void 마감일_형식이_잘못되면_해당_구분만_제외하고_계속_진행한다() {
        QNetScheduleItem invalidWrittenDeadline = new QNetScheduleItem("정보처리기사", "2026년 정기 기사 1회",
                "20260112", "2026-01-15", "20260130", "20260303", "20260311",
                "20260323", "20260326", "20260418", "20260506", "20260612", "20260811",
                "정보통신", "정보기술");
        when(client.fetchQualificationList(5000, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("1320", "정보처리기사", "T")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(invalidWrittenDeadline));

        List<ExternalPostingData> result = source.fetchPostings();

        // 필기는 마감일 파싱 실패로 제외되고 실기만 남는다.
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).contains("실기시험");
    }

    @Test
    void 특정_종목_조회가_실패하면_해당_종목만_건너뛰고_나머지는_계속_처리한다() {
        when(client.fetchQualificationList(5000, 1)).thenReturn(qualificationList(
                new QNetQualificationItem("1320", "정보처리기사", "T"),
                new QNetQualificationItem("0752", "가스기술사", "T")));
        when(client.fetchSchedule("1320")).thenThrow(new RuntimeException("API 오류"));
        when(client.fetchSchedule("0752")).thenReturn(scheduleResponse(fullScheduleItem("기술사(2026년도 제138회)")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(d -> assertThat(d.externalId()).startsWith("0752-"));
    }

    @Test
    void 계속_실패하는_종목은_최대_5회까지_재시도한_뒤_포기한다() {
        when(client.fetchQualificationList(5000, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("1320", "정보처리기사", "T")));
        when(client.fetchSchedule("1320")).thenThrow(new RuntimeException("API 오류"));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).isEmpty();
        verify(client, org.mockito.Mockito.times(5)).fetchSchedule("1320");
    }

    @Test
    void 종목이_3건_연속으로_완전히_실패하면_서킷브레이커가_동작해_길게_대기한_뒤_계속한다() {
        when(client.fetchQualificationList(5000, 1)).thenReturn(qualificationList(
                new QNetQualificationItem("1320", "정보처리기사", "T"),
                new QNetQualificationItem("0752", "가스기술사", "T"),
                new QNetQualificationItem("0080", "건설기계기술사", "T"),
                new QNetQualificationItem("0740", "건설안전기술사", "T")));
        when(client.fetchSchedule("1320")).thenThrow(new RuntimeException("API 오류"));
        when(client.fetchSchedule("0752")).thenThrow(new RuntimeException("API 오류"));
        when(client.fetchSchedule("0080")).thenThrow(new RuntimeException("API 오류"));
        when(client.fetchSchedule("0740")).thenReturn(scheduleResponse(fullScheduleItem("2026년도 1회")));

        List<Long> sleptMillis = new java.util.ArrayList<>();
        source.setSleeperForTest(sleptMillis::add);

        List<ExternalPostingData> result = source.fetchPostings();

        // 3번째 종목까지 연속 실패한 직후 30초(COOLDOWN_MILLIS) 대기가 기록되고, 4번째 종목은 정상 수집된다.
        assertThat(sleptMillis).contains(30_000L);
        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(d -> assertThat(d.externalId()).startsWith("0740-"));
    }
}
