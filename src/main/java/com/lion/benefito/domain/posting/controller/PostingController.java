package com.lion.benefito.domain.posting.controller;

import com.lion.benefito.domain.posting.dto.CalendarDeadlineCountResponse;
import com.lion.benefito.domain.posting.dto.PostingDetailResponse;
import com.lion.benefito.domain.posting.dto.PostingListResponse;
import com.lion.benefito.domain.posting.dto.PostingPageResponse;
import com.lion.benefito.domain.posting.dto.PostingSearchCondition;
import com.lion.benefito.domain.posting.entity.PostingFilter;
import com.lion.benefito.domain.posting.entity.PostingSort;
import com.lion.benefito.domain.posting.service.PostingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Posting", description = "공고 API")
@RestController
@RequiredArgsConstructor
public class PostingController {

    private final PostingService postingService;

    @Operation(summary = "공고 목록 조회", description = "키워드 검색, 지역/카테고리 필터, 정렬, 추천 필터, 페이지네이션을 지원하는 공고 목록 조회")
    @GetMapping("/postings")
    public ResponseEntity<PostingPageResponse> getPostings(
            @Parameter(description = "검색 키워드 (title, company 대상, 대소문자 무시)") @RequestParam(required = false) String keyword,
            @Parameter(description = "지역 코드 (예: SEOUL)") @RequestParam(required = false) String region,
            @Parameter(description = "카테고리 코드 (예: JOB)") @RequestParam(required = false) String category,
            @Parameter(description = "정렬 기준 (deadline, popular, latest), 기본값 latest") @RequestParam(required = false) String sort,
            @Parameter(description = "필터 (all, recommended), 기본값 all") @RequestParam(required = false) String filter,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        PostingSort postingSort = PostingSort.from(sort);
        PostingFilter postingFilter = PostingFilter.from(filter);
        PostingSearchCondition condition = new PostingSearchCondition(keyword, region, category, postingFilter);
        Pageable pageable = PageRequest.of(page, size, postingSort.toSort());

        return ResponseEntity.ok(postingService.getPostings(condition, pageable));
    }

    @Operation(summary = "공고 상세 조회", description = "공고 ID로 상세 정보를 조회한다.")
    @GetMapping("/postings/{postingId}")
    public ResponseEntity<PostingDetailResponse> getPosting(@PathVariable Long postingId) {
        return ResponseEntity.ok(postingService.getPosting(postingId));
    }

    @Operation(summary = "공고 마감 캘린더 조회", description = "yearMonth(yyyy-MM)에 해당하는 월의 날짜별 마감 공고 개수를 반환한다.")
    @GetMapping("/postings/calendar")
    public ResponseEntity<List<CalendarDeadlineCountResponse>> getCalendar(
            @Parameter(description = "조회할 연월 (yyyy-MM)", example = "2026-07", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {
        return ResponseEntity.ok(postingService.getCalendar(yearMonth));
    }

    @Operation(summary = "특정 날짜 마감 공고 목록 조회", description = "date(yyyy-MM-dd)에 마감되는 공고 목록을 조회한다. 없으면 빈 배열을 반환한다.")
    @GetMapping("/postings/calendar/announcements")
    public ResponseEntity<List<PostingListResponse>> getPostingsByDeadline(
            @Parameter(description = "조회할 마감일 (yyyy-MM-dd)", example = "2026-07-15", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(postingService.getPostingsByDeadline(date));
    }

    @Operation(summary = "공고 조회수 증가", description = "해당 공고의 조회수를 1 증가시킨다. 변경된 조회수는 응답 본문에 포함하지 않는다.")
    @PatchMapping("/postings/{postingId}/views")
    public ResponseEntity<Void> increaseViewCount(@PathVariable Long postingId) {
        postingService.increaseViewCount(postingId);
        return ResponseEntity.noContent().build();
    }
}
