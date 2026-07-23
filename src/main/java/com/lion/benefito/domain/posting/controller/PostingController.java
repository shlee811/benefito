package com.lion.benefito.domain.posting.controller;

import com.lion.benefito.domain.posting.dto.PostingCalendarDetailPageResponse;
import com.lion.benefito.domain.posting.dto.PostingCalendarResponse;
import com.lion.benefito.domain.posting.dto.PostingDetailResponse;
import com.lion.benefito.domain.posting.dto.PostingListResponse;
import com.lion.benefito.domain.posting.dto.PostingPageResponse;
import com.lion.benefito.domain.posting.dto.PostingSearchCondition;
import com.lion.benefito.domain.posting.entity.CalendarEventType;
import com.lion.benefito.domain.posting.entity.PostingFilter;
import com.lion.benefito.domain.posting.entity.PostingSort;
import com.lion.benefito.domain.posting.entity.PostingStatus;
import com.lion.benefito.domain.posting.service.PostingService;
import com.lion.benefito.global.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Posting", description = "공고 API")
@RestController
@RequiredArgsConstructor
public class PostingController {

    private final PostingService postingService;
    private final CurrentUserProvider currentUserProvider;

    @Operation(summary = "공고 목록 조회",
            description = "키워드 검색, 지역/카테고리 필터, 정렬, 추천 필터, 페이지네이션을 지원하는 공고 목록 조회. "
                    + "로그인 사용자가 filter=recommended로 조회하면 관심 카테고리와 일치하는 공고를 우선 반환하고, "
                    + "부족하면 기존 마감임박 기준으로 채운다. 비로그인/관심사 미설정 사용자는 기존 기준만 적용된다.")
    @GetMapping("/postings")
    public ResponseEntity<PostingPageResponse> getPostings(
            @Parameter(description = "검색 키워드 (title, company 대상, 대소문자 무시)") @RequestParam(required = false) String keyword,
            @Parameter(description = "지역 코드 (예: SEOUL)") @RequestParam(required = false) String region,
            @Parameter(description = "카테고리 코드 (예: JOB)") @RequestParam(required = false) String category,
            @Parameter(description = "정렬 기준 (deadline, popular, latest), 기본값 latest") @RequestParam(required = false) String sort,
            @Parameter(description = "필터 (all, recommended), 기본값 all") @RequestParam(required = false) String filter,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        log.info("[ PostingController ] 공고 목록 조회 요청. keyword={}, region={}, category={}, sort={}, filter={}, "
                        + "page={}, size={}",
                keyword, region, category, sort, filter, page, size);

        PostingSort postingSort = PostingSort.from(sort);
        PostingFilter postingFilter = PostingFilter.from(filter);
        PostingSearchCondition condition = new PostingSearchCondition(keyword, region, category, postingFilter);
        Pageable pageable = PageRequest.of(page, size, postingSort.toSort());
        Long currentUserId = currentUserProvider.getCurrentUserIdOrEmpty().orElse(null);

        PostingPageResponse response = postingService.getPostings(condition, pageable, currentUserId);
        log.info("[ PostingController ] 공고 목록 조회 결과. totalElements={}, 이번 페이지 건수={}",
                response.totalElements(), response.content().size());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "공고 상세 조회", description = "공고 ID로 상세 정보를 조회한다.")
    @GetMapping("/postings/{postingId}")
    public ResponseEntity<PostingDetailResponse> getPosting(@PathVariable Long postingId) {
        return ResponseEntity.ok(postingService.getPosting(postingId));
    }

    @Operation(summary = "특정 날짜 마감 공고 목록 조회(레거시)", description = "date(yyyy-MM-dd)에 마감되는 공고 목록을 조회한다. 없으면 빈 배열을 반환한다.")
    @GetMapping("/postings/calendar/announcements")
    public ResponseEntity<List<PostingListResponse>> getPostingsByDeadline(
            @Parameter(description = "조회할 마감일 (yyyy-MM-dd)", example = "2026-07-15", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(postingService.getPostingsByDeadline(date));
    }

    @Operation(summary = "공고 캘린더 월별 상세 조회",
            description = "year/month에 해당하는 월의 날짜별 시작/마감 공고 집계와 최대 5개 미리보기(items)를 반환한다. "
                    + "시작(START) 기준은 Posting에 아직 실제 게시/시작일 필드가 없어 createdAt의 날짜 부분을 임시로 사용한다. "
                    + "status를 지정하지 않으면 ACTIVE/CLOSED 모두 이력으로 포함되고, 지정하면(ACTIVE 또는 CLOSED) 해당 상태만 집계한다.")
    @GetMapping("/postings/calendar")
    public ResponseEntity<PostingCalendarResponse> getPostingCalendar(
            @Parameter(description = "연도", example = "2026", required = true) @RequestParam int year,
            @Parameter(description = "월(1~12)", example = "7", required = true) @RequestParam int month,
            @Parameter(description = "카테고리 코드 (예: JOB)") @RequestParam(required = false) String category,
            @Parameter(description = "지역 코드 (예: SEOUL)") @RequestParam(required = false) String region,
            @Parameter(description = "필터 (all, recommended), 기본값 all") @RequestParam(required = false) String filter,
            @Parameter(description = "공고 상태(ACTIVE, CLOSED). 미지정 시 상태 무관 전체 이력") @RequestParam(required = false) String status) {

        PostingFilter postingFilter = PostingFilter.from(filter);
        PostingStatus postingStatus = PostingStatus.from(status);
        Long currentUserId = currentUserProvider.getCurrentUserIdOrEmpty().orElse(null);

        PostingCalendarResponse response = postingService.getCalendar(
                year, month, category, region, postingFilter, postingStatus, currentUserId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "공고 캘린더 날짜별 상세 목록 조회",
            description = "date(yyyy-MM-dd)에 시작하거나 마감되는 공고 전체 목록을 페이지네이션으로 조회한다. "
                    + "type=START/DEADLINE으로 좁힐 수 있고, category/region/filter=recommended/status와 조합 가능하다.")
    @GetMapping("/postings/calendar/{date}")
    public ResponseEntity<PostingCalendarDetailPageResponse> getPostingCalendarDayDetail(
            @Parameter(description = "조회할 날짜 (yyyy-MM-dd)", example = "2026-07-01", required = true)
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "이벤트 타입(START, DEADLINE). 미지정 시 둘 다 포함") @RequestParam(required = false) String type,
            @Parameter(description = "카테고리 코드 (예: JOB)") @RequestParam(required = false) String category,
            @Parameter(description = "지역 코드 (예: SEOUL)") @RequestParam(required = false) String region,
            @Parameter(description = "필터 (all, recommended), 기본값 all") @RequestParam(required = false) String filter,
            @Parameter(description = "공고 상태(ACTIVE, CLOSED). 미지정 시 상태 무관 전체 이력") @RequestParam(required = false) String status,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        CalendarEventType eventType = CalendarEventType.from(type);
        PostingFilter postingFilter = PostingFilter.from(filter);
        PostingStatus postingStatus = PostingStatus.from(status);
        Pageable pageable = PageRequest.of(page, size);
        Long currentUserId = currentUserProvider.getCurrentUserIdOrEmpty().orElse(null);

        PostingCalendarDetailPageResponse response = postingService.getCalendarDayDetail(
                date, eventType, category, region, postingFilter, postingStatus, pageable, currentUserId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "공고 조회수 증가", description = "해당 공고의 조회수를 1 증가시킨다. 변경된 조회수는 응답 본문에 포함하지 않는다.")
    @PatchMapping("/postings/{postingId}/views")
    public ResponseEntity<Void> increaseViewCount(@PathVariable Long postingId) {
        postingService.increaseViewCount(postingId);
        return ResponseEntity.noContent().build();
    }
}
