package com.lion.benefito.domain.posting.service;

import com.lion.benefito.domain.posting.exception.PostingErrorCode;
import com.lion.benefito.domain.posting.exception.PostingException;
import com.lion.benefito.domain.posting.dto.CalendarDeadlineCountResponse;
import com.lion.benefito.domain.posting.dto.PostingDetailResponse;
import com.lion.benefito.domain.posting.dto.PostingListResponse;
import com.lion.benefito.domain.posting.dto.PostingPageResponse;
import com.lion.benefito.domain.posting.dto.PostingSearchCondition;
import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.domain.posting.entity.PostingFilter;
import com.lion.benefito.domain.posting.entity.PostingSort;
import com.lion.benefito.domain.posting.repository.PostingRepository;
import com.lion.benefito.domain.posting.repository.PostingSpecification;
import com.lion.benefito.domain.user.repository.UserInterestRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostingService {

    private final PostingRepository postingRepository;
    private final UserInterestRepository userInterestRepository;

    public PostingPageResponse getPostings(PostingSearchCondition condition, Pageable pageable, Long currentUserId) {
        List<Long> interestCategoryIds = resolveInterestCategoryIds(currentUserId);

        if (condition.filter() == PostingFilter.RECOMMENDED && !interestCategoryIds.isEmpty()) {
            return getPersonalizedRecommendedPostings(condition, pageable, interestCategoryIds);
        }

        Page<Posting> postings = postingRepository.findAll(PostingSpecification.search(condition), pageable);
        return PostingPageResponse.from(postings.map(posting -> PostingListResponse.from(posting, interestCategoryIds)));
    }

    // 비로그인이면 빈 리스트(개인화 없음), 로그인했지만 관심사가 없으면 역시 빈 리스트(기존 로직 fallback).
    private List<Long> resolveInterestCategoryIds(Long currentUserId) {
        if (currentUserId == null) {
            return List.of();
        }
        return userInterestRepository.findByUserId(currentUserId).stream()
                .map(interest -> interest.getCategoryCode().getId())
                .toList();
    }

    // 1순위: 관심 카테고리와 일치하는 ACTIVE 공고. 2순위(fallback): 기존 마감임박 기준.
    // 두 결과를 애플리케이션에서 직접 합치고 잘라내는 이유는, "관심사 일치를 우선시하고 부족하면
    // 기존 기준으로 채운다"는 요구사항이 단순 OR 조건 하나로는 우선순위(정렬)까지 표현할 수 없기
    // 때문이다(Specification 안에서 세운 orderBy는 Pageable의 Sort로 덮어써진다).
    private PostingPageResponse getPersonalizedRecommendedPostings(
            PostingSearchCondition condition, Pageable pageable, List<Long> interestCategoryIds) {
        List<Posting> interestMatches = postingRepository.findAll(
                PostingSpecification.searchByInterestCategories(condition, interestCategoryIds), pageable.getSort());

        Set<Long> matchedIds = new HashSet<>();
        for (Posting posting : interestMatches) {
            matchedIds.add(posting.getId());
        }

        List<Posting> fallbackCandidates = postingRepository.findAll(
                PostingSpecification.search(condition), pageable.getSort());
        List<Posting> fallback = fallbackCandidates.stream()
                .filter(posting -> !matchedIds.contains(posting.getId()))
                .toList();

        List<Posting> combined = new ArrayList<>(interestMatches);
        combined.addAll(fallback);

        int start = Math.min((int) pageable.getOffset(), combined.size());
        int end = Math.min(start + pageable.getPageSize(), combined.size());
        Page<Posting> page = new PageImpl<>(combined.subList(start, end), pageable, combined.size());

        return PostingPageResponse.from(page.map(posting -> PostingListResponse.from(posting, interestCategoryIds)));
    }

    public PostingDetailResponse getPosting(Long postingId) {
        Posting posting = postingRepository.findById(postingId)
                .orElseThrow(() -> new PostingException(PostingErrorCode.POSTING_NOT_FOUND));
        return PostingDetailResponse.from(posting);
    }

    public List<CalendarDeadlineCountResponse> getCalendar(YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.plusMonths(1).atDay(1);
        return postingRepository.countDeadlinesByEndDateBetween(startDate, endDate);
    }

    public List<PostingListResponse> getPostingsByDeadline(LocalDate date) {
        return postingRepository.findByEndDate(date, PostingSort.LATEST.toSort())
                .stream()
                .map(PostingListResponse::from)
                .toList();
    }

    @Transactional
    public void increaseViewCount(Long postingId) {
        int updatedCount = postingRepository.incrementViewCount(postingId);
        if (updatedCount == 0) {
            throw new PostingException(PostingErrorCode.POSTING_NOT_FOUND);
        }
    }
}
