package com.lion.benefito.domain.posting.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.lion.benefito.domain.code.entity.Code;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PostingTest {

    private Posting posting(LocalDate endDate, PostingStatus status) {
        Posting posting = Posting.builder()
                .categoryCode(org.mockito.Mockito.mock(Code.class))
                .title("제목")
                .company("회사")
                .endDate(endDate)
                .status(status)
                .build();
        return posting;
    }

    @Test
    void ACTIVE이고_마감일이_오늘로부터_7일_이내면_추천이다() {
        Posting posting = posting(LocalDate.now().plusDays(7), PostingStatus.ACTIVE);

        assertThat(posting.isRecommended()).isTrue();
    }

    @Test
    void ACTIVE이고_마감일이_오늘이면_추천이다() {
        Posting posting = posting(LocalDate.now(), PostingStatus.ACTIVE);

        assertThat(posting.isRecommended()).isTrue();
    }

    @Test
    void ACTIVE이어도_마감일이_8일_이후면_추천이_아니다() {
        Posting posting = posting(LocalDate.now().plusDays(8), PostingStatus.ACTIVE);

        assertThat(posting.isRecommended()).isFalse();
    }

    @Test
    void 마감일이_임박해도_CLOSED면_추천이_아니다() {
        Posting posting = posting(LocalDate.now().plusDays(1), PostingStatus.CLOSED);

        assertThat(posting.isRecommended()).isFalse();
    }

    @Test
    void 마감일이_없으면_추천이_아니다() {
        Posting posting = posting(null, PostingStatus.ACTIVE);

        assertThat(posting.isRecommended()).isFalse();
    }

    @Test
    void 마감일이_이미_지났으면_추천이_아니다() {
        Posting posting = posting(LocalDate.now().minusDays(1), PostingStatus.ACTIVE);

        assertThat(posting.isRecommended()).isFalse();
    }
}
