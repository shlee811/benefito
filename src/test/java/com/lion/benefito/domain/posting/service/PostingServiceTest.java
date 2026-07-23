package com.lion.benefito.domain.posting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.posting.dto.PostingPageResponse;
import com.lion.benefito.domain.posting.dto.PostingSearchCondition;
import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.domain.posting.entity.PostingFilter;
import com.lion.benefito.domain.posting.entity.PostingStatus;
import com.lion.benefito.domain.posting.repository.PostingRepository;
import com.lion.benefito.domain.posting.repository.PostingSpecification;
import com.lion.benefito.domain.user.entity.UserInterest;
import com.lion.benefito.domain.user.repository.UserInterestRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

class PostingServiceTest {

    private PostingRepository postingRepository;
    private UserInterestRepository userInterestRepository;
    private PostingService postingService;

    @BeforeEach
    void setUp() {
        postingRepository = mock(PostingRepository.class);
        userInterestRepository = mock(UserInterestRepository.class);
        postingService = new PostingService(postingRepository, userInterestRepository);
    }

    private Posting posting(Long id, Code categoryCode) {
        Posting posting = Posting.builder()
                .categoryCode(categoryCode)
                .title("제목" + id)
                .company("회사")
                .status(PostingStatus.ACTIVE)
                .endDate(LocalDate.now().plusDays(1))
                .build();
        setId(posting, id);
        return posting;
    }

    private Code code(Long id) {
        Code code = mock(Code.class);
        when(code.getId()).thenReturn(id);
        return code;
    }

    private UserInterest interest(Code categoryCode) {
        UserInterest interest = mock(UserInterest.class);
        when(interest.getCategoryCode()).thenReturn(categoryCode);
        return interest;
    }

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void 비로그인_사용자는_관심사_조회_없이_기존_전역_추천_기준만_적용된다() {
        PostingSearchCondition condition = new PostingSearchCondition(null, null, null, PostingFilter.RECOMMENDED);
        Pageable pageable = PageRequest.of(0, 20);
        when(postingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty(pageable));

        postingService.getPostings(condition, pageable, null);

        verify(userInterestRepository, never()).findByUserId(any());
    }

    @Test
    void 로그인했지만_관심사가_없으면_기존_전역_추천_기준으로_fallback한다() {
        PostingSearchCondition condition = new PostingSearchCondition(null, null, null, PostingFilter.RECOMMENDED);
        Pageable pageable = PageRequest.of(0, 20);
        when(userInterestRepository.findByUserId(1L)).thenReturn(List.of());
        when(postingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty(pageable));

        postingService.getPostings(condition, pageable, 1L);

        verify(postingRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void 관심_카테고리와_일치하는_공고가_우선_반환되고_isRecommended가_true다() {
        Code jobCode = code(100L);
        Code scholarshipCode = code(200L);
        Posting interestMatch = posting(1L, jobCode);
        Posting other = posting(2L, scholarshipCode);

        PostingSearchCondition condition = new PostingSearchCondition(null, null, null, PostingFilter.RECOMMENDED);
        Pageable pageable = PageRequest.of(0, 20);

        UserInterest jobInterest = interest(jobCode);
        when(userInterestRepository.findByUserId(1L)).thenReturn(List.of(jobInterest));
        when(postingRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(interestMatch))
                .thenReturn(List.of(interestMatch, other));

        PostingPageResponse response = postingService.getPostings(condition, pageable, 1L);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).id()).isEqualTo(1L);
        assertThat(response.content().get(0).isRecommended()).isTrue();
        assertThat(response.content().get(1).id()).isEqualTo(2L);
    }

    @Test
    void 관심_카테고리_매칭이_부족하면_기존_마감임박_기준으로_나머지를_채운다() {
        Code jobCode = code(100L);
        Code scholarshipCode = code(200L);
        Posting interestMatch = posting(1L, jobCode);
        Posting fallbackOnly = posting(2L, scholarshipCode);

        PostingSearchCondition condition = new PostingSearchCondition(null, null, null, PostingFilter.RECOMMENDED);
        Pageable pageable = PageRequest.of(0, 20);

        UserInterest jobInterest = interest(jobCode);
        when(userInterestRepository.findByUserId(1L)).thenReturn(List.of(jobInterest));
        when(postingRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(interestMatch))
                .thenReturn(List.of(interestMatch, fallbackOnly));

        PostingPageResponse response = postingService.getPostings(condition, pageable, 1L);

        List<Long> ids = response.content().stream().map(item -> item.id()).toList();
        assertThat(ids).containsExactly(1L, 2L);
        assertThat(response.totalElements()).isEqualTo(2);
    }

    @Test
    void filter가_all이어도_로그인_사용자의_관심_카테고리와_일치하면_isRecommended가_true다() {
        Code jobCode = code(100L);
        Posting matching = posting(1L, jobCode);

        PostingSearchCondition condition = new PostingSearchCondition(null, null, null, PostingFilter.ALL);
        Pageable pageable = PageRequest.of(0, 20);

        UserInterest jobInterest = interest(jobCode);
        when(userInterestRepository.findByUserId(1L)).thenReturn(List.of(jobInterest));
        when(postingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(matching), pageable, 1));

        PostingPageResponse response = postingService.getPostings(condition, pageable, 1L);

        assertThat(response.content().get(0).isRecommended()).isTrue();
    }
}
