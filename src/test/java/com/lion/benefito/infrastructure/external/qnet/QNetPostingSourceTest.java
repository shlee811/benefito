package com.lion.benefito.infrastructure.external.qnet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lion.benefito.infrastructure.external.ExternalPostingData;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QNetPostingSourceTest {

    private QNetClient client;
    private QNetPostingSource source;
    private QNetProperties properties;

    @BeforeEach
    void setUp() {
        client = mock(QNetClient.class);
        properties = new QNetProperties();
        source = new QNetPostingSource(client, properties);
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

    private QNetScheduleItem scheduleItem(String implplannm, String docRegEndDt) {
        return new QNetScheduleItem("정보처리기사", implplannm,
                "20260112", docRegEndDt, "20260130", "20260303", "20260311",
                "20260323", "20260326", "20260418", "20260506", "20260612", "20260811",
                "정보통신", "정보기술");
    }

    @Test
    void target_codes에_해당하는_종목만_시험일정을_조회한다() {
        properties.setTargetCodes(List.of("1320"));
        when(client.fetchQualificationList(700, 1)).thenReturn(qualificationList(
                new QNetQualificationItem("1320", "정보처리기사"),
                new QNetQualificationItem("0752", "가스기술사")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(scheduleItem("2026년 정기 기사 1회", "20260115")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).hasSize(1);
        verify(client, never()).fetchSchedule("0752");
    }

    @Test
    void sourceType과_externalId를_올바르게_채운다() {
        properties.setTargetCodes(List.of("1320"));
        when(client.fetchQualificationList(700, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("1320", "정보처리기사")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(scheduleItem("2026년 정기 기사 1회", "20260115")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).sourceType()).isEqualTo("QNET");
        assertThat(result.get(0).externalId()).isEqualTo("1320-2026년 정기 기사 1회");
    }

    @Test
    void title은_종목명과_회차를_조합한다() {
        properties.setTargetCodes(List.of("1320"));
        when(client.fetchQualificationList(700, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("1320", "정보처리기사")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(scheduleItem("2026년 정기 기사 1회", "20260115")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).title()).isEqualTo("정보처리기사 2026년 정기 기사 1회");
    }

    @Test
    void endDate는_필기시험_원서접수_종료일로_yyyyMMdd_파싱한다() {
        properties.setTargetCodes(List.of("1320"));
        when(client.fetchQualificationList(700, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("1320", "정보처리기사")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(scheduleItem("2026년 정기 기사 1회", "20260115")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).deadline()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @Test
    void qualification은_분야_태그와_대직무분야_중직무분야_조합을_함께_담는다() {
        properties.setTargetCodes(List.of("1320"));
        when(client.fetchQualificationList(700, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("1320", "정보처리기사")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(scheduleItem("2026년 정기 기사 1회", "20260115")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).qualification()).isEqualTo("분야: 정보처리기사\n정보통신 > 정보기술");
    }

    @Test
    void region은_항상_전국으로_채운다() {
        properties.setTargetCodes(List.of("1320"));
        when(client.fetchQualificationList(700, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("1320", "정보처리기사")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(scheduleItem("2026년 정기 기사 1회", "20260115")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).region()).isEqualTo("전국");
    }

    @Test
    void content는_접수_시험_발표_일정을_사람이_읽기_좋은_형태로_담는다() {
        properties.setTargetCodes(List.of("1320"));
        when(client.fetchQualificationList(700, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("1320", "정보처리기사")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(scheduleItem("2026년 정기 기사 1회", "20260115")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result.get(0).content()).contains("필기시험 원서접수: 20260112 ~ 20260115");
        assertThat(result.get(0).content()).contains("합격자 발표: 20260612 ~ 20260811");
    }

    @Test
    void 마감일_형식이_잘못되면_해당_항목만_제외하고_계속_진행한다() {
        properties.setTargetCodes(List.of("1320"));
        when(client.fetchQualificationList(700, 1))
                .thenReturn(qualificationList(new QNetQualificationItem("1320", "정보처리기사")));
        when(client.fetchSchedule("1320")).thenReturn(scheduleResponse(
                scheduleItem("2026년 정기 기사 1회", "2026-01-15"),
                scheduleItem("2026년 정기 기사 2회", "20260420")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).externalId()).isEqualTo("1320-2026년 정기 기사 2회");
    }

    @Test
    void 특정_종목_조회가_실패하면_해당_종목만_건너뛰고_나머지는_계속_처리한다() {
        properties.setTargetCodes(List.of("1320", "0752"));
        when(client.fetchQualificationList(700, 1)).thenReturn(qualificationList(
                new QNetQualificationItem("1320", "정보처리기사"),
                new QNetQualificationItem("0752", "가스기술사")));
        when(client.fetchSchedule("1320")).thenThrow(new RuntimeException("API 오류"));
        when(client.fetchSchedule("0752")).thenReturn(scheduleResponse(scheduleItem("기술사(2026년도 제138회)", "20260109")));

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).externalId()).isEqualTo("0752-기술사(2026년도 제138회)");
    }

    @Test
    void target_codes가_비어있으면_아무것도_조회하지_않는다() {
        properties.setTargetCodes(List.of());

        List<ExternalPostingData> result = source.fetchPostings();

        assertThat(result).isEmpty();
        verify(client, never()).fetchQualificationList(anyInt(), anyInt());
    }
}
