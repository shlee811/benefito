package com.lion.benefito.domain.posting.service;

import com.lion.benefito.domain.posting.exception.PostingErrorCode;
import com.lion.benefito.domain.posting.exception.PostingException;
import com.lion.benefito.domain.posting.dto.PostingCalendarDayResponse;
import com.lion.benefito.domain.posting.dto.PostingCalendarDetailPageResponse;
import com.lion.benefito.domain.posting.dto.PostingCalendarDetailResponse;
import com.lion.benefito.domain.posting.dto.PostingCalendarItemResponse;
import com.lion.benefito.domain.posting.dto.PostingCalendarResponse;
import com.lion.benefito.domain.posting.dto.PostingDetailResponse;
import com.lion.benefito.domain.posting.dto.PostingListResponse;
import com.lion.benefito.domain.posting.dto.PostingPageResponse;
import com.lion.benefito.domain.posting.dto.PostingSearchCondition;
import com.lion.benefito.domain.posting.entity.CalendarEventType;
import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.domain.posting.entity.PostingFilter;
import com.lion.benefito.domain.posting.entity.PostingSort;
import com.lion.benefito.domain.posting.entity.PostingStatus;
import com.lion.benefito.domain.posting.repository.PostingRepository;
import com.lion.benefito.domain.posting.repository.PostingSpecification;
import com.lion.benefito.domain.user.repository.UserInterestRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    // 월별 캘린더 상세(날짜별 시작/마감 집계 + 최대 5개 미리보기). 월 범위로 한정된 공고만 DB에서
    // 조회하고(테이블 전체 로딩 없음), 날짜별 그룹핑/상위 5개 추리기는 애플리케이션에서 수행한다
    // (날짜별 "상위 5개"를 DB에서 바로 뽑는 쿼리는 윈도우 함수가 필요해 복잡도가 크므로, 월 범위로
    // 이미 좁혀진 소량의 후보군에 한해 이 방식을 택했다).
    public PostingCalendarResponse getCalendar(
            int year, int month, String category, String region, PostingFilter filter,
            PostingStatus status, Long currentUserId) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEndExclusive = yearMonth.plusMonths(1).atDay(1);
        LocalDateTime monthStartDateTime = monthStart.atStartOfDay();
        LocalDateTime monthEndDateTimeExclusive = monthEndExclusive.atStartOfDay();

        boolean recommendedOnly = filter == PostingFilter.RECOMMENDED;
        List<Long> interestCategoryIds = recommendedOnly ? resolveInterestCategoryIds(currentUserId) : List.of();

        List<Posting> startCandidates = postingRepository.findAll(PostingSpecification.calendarStart(
                monthStartDateTime, monthEndDateTimeExclusive, category, region, status,
                recommendedOnly, interestCategoryIds));
        List<Posting> deadlineCandidates = postingRepository.findAll(PostingSpecification.calendarDeadline(
                monthStart, monthEndExclusive, category, region, status, recommendedOnly, interestCategoryIds));

        Map<LocalDate, List<PostingCalendarItemResponse>> startItemsByDate =
                groupByDate(startCandidates, CalendarEventType.START, posting -> posting.getCreatedAt().toLocalDate());
        Map<LocalDate, List<PostingCalendarItemResponse>> deadlineItemsByDate =
                groupByDate(deadlineCandidates, CalendarEventType.DEADLINE, Posting::getEndDate);

        Set<LocalDate> allDates = new TreeSet<>();
        allDates.addAll(startItemsByDate.keySet());
        allDates.addAll(deadlineItemsByDate.keySet());

        List<PostingCalendarDayResponse> days = allDates.stream()
                .map(date -> buildDay(date,
                        startItemsByDate.getOrDefault(date, List.of()),
                        deadlineItemsByDate.getOrDefault(date, List.of())))
                .toList();

        return PostingCalendarResponse.from(year, month, days);
    }

    private Map<LocalDate, List<PostingCalendarItemResponse>> groupByDate(
            List<Posting> postings, CalendarEventType type, Function<Posting, LocalDate> dateExtractor) {
        return postings.stream()
                .sorted(Comparator.comparing(Posting::getId))
                .collect(Collectors.groupingBy(
                        dateExtractor,
                        TreeMap::new,
                        Collectors.mapping(posting -> PostingCalendarItemResponse.from(posting, type), Collectors.toList())));
    }

    // 미리보기 정렬 정책: START 항목을 먼저, DEADLINE 항목을 뒤에 배치하고(요구사항 4), 각 그룹
    // 내부는 postingId 오름차순(결정적 순서 보장). 최대 5개까지만 items에 담고 나머지는
    // remainingCount로 안내한다.
    private PostingCalendarDayResponse buildDay(
            LocalDate date, List<PostingCalendarItemResponse> startItems, List<PostingCalendarItemResponse> deadlineItems) {
        long startCount = startItems.size();
        long deadlineCount = deadlineItems.size();
        long totalCount = startCount + deadlineCount;

        List<PostingCalendarItemResponse> combined = new ArrayList<>(startItems);
        combined.addAll(deadlineItems);
        List<PostingCalendarItemResponse> preview = combined.stream().limit(5).toList();
        long remainingCount = Math.max(0, totalCount - preview.size());

        return new PostingCalendarDayResponse(date, startCount, deadlineCount, totalCount, preview, remainingCount);
    }

    // 날짜 클릭 시 전체 목록(페이지네이션 적용). 하루 단위로만 후보를 조회하므로 역시 테이블 전체
    // 로딩 없이 소량의 범위 조회 + 애플리케이션 페이지네이션으로 처리한다.
    public PostingCalendarDetailPageResponse getCalendarDayDetail(
            LocalDate date, CalendarEventType type, String category, String region, PostingFilter filter,
            PostingStatus status, Pageable pageable, Long currentUserId) {
        boolean recommendedOnly = filter == PostingFilter.RECOMMENDED;
        List<Long> interestCategoryIds = recommendedOnly ? resolveInterestCategoryIds(currentUserId) : List.of();

        List<PostingCalendarDetailResponse> combined = new ArrayList<>();
        if (type == null || type == CalendarEventType.START) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEndExclusive = date.plusDays(1).atStartOfDay();
            postingRepository.findAll(PostingSpecification.calendarStart(
                            dayStart, dayEndExclusive, category, region, status, recommendedOnly, interestCategoryIds))
                    .stream()
                    .sorted(Comparator.comparing(Posting::getId))
                    .forEach(posting -> combined.add(PostingCalendarDetailResponse.from(posting, CalendarEventType.START)));
        }
        if (type == null || type == CalendarEventType.DEADLINE) {
            postingRepository.findAll(PostingSpecification.calendarDeadline(
                            date, date.plusDays(1), category, region, status, recommendedOnly, interestCategoryIds))
                    .stream()
                    .sorted(Comparator.comparing(Posting::getId))
                    .forEach(posting -> combined.add(PostingCalendarDetailResponse.from(posting, CalendarEventType.DEADLINE)));
        }

        int start = Math.min((int) pageable.getOffset(), combined.size());
        int end = Math.min(start + pageable.getPageSize(), combined.size());
        Page<PostingCalendarDetailResponse> page = new PageImpl<>(combined.subList(start, end), pageable, combined.size());
        return PostingCalendarDetailPageResponse.from(page);
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
