package com.lion.benefito.domain.scrap.controller;

import com.lion.benefito.domain.scrap.dto.response.ScrapResDTO;
import com.lion.benefito.domain.scrap.service.command.ScrapCommandService;
import com.lion.benefito.domain.scrap.service.query.ScrapQueryService;
import com.lion.benefito.global.common.CustomResponse;
import com.lion.benefito.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Scrap API", description = "공고 스크랩 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/scraps")
public class ScrapController {

    private final ScrapCommandService scrapCommandService;
    private final ScrapQueryService scrapQueryService;

    @Operation(summary = "내 스크랩 목록 조회", description = "로그인 사용자의 스크랩한 공고 목록을 조회한다. 카테고리 필터링이 가능하다.")
    @GetMapping("/my")
    public CustomResponse<ScrapResDTO.MyScrapList> getMyScrapList(
            @RequestParam(required = false) String categoryCode,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        ScrapResDTO.MyScrapList response = scrapQueryService.getMyScrapList(userId, categoryCode);

        return CustomResponse.onSuccess(response);
    }

    @Operation(summary = "이번 주 마감 스크랩 목록 조회", description = "로그인 사용자의 스크랩한 공고 중 이번 주 일요일까지 마감인 공고 목록을 조회한다. 카테고리 필터링이 가능하다.")
    @GetMapping("/my/weekly-deadline")
    public CustomResponse<ScrapResDTO.MyScrapList> getWeeklyDeadlineScrapList(
            @RequestParam(required = false) String categoryCode,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long userId = userDetails.getUser().getId();
        ScrapResDTO.MyScrapList response = scrapQueryService.getWeeklyDeadlineScrapList(userId, categoryCode);
        
        return CustomResponse.onSuccess(response);
    }

    @Operation(summary = "내 스크랩 요약 통계 조회", description = "로그인 사용자의 전체 스크랩 수와 이번 주 마감 공고 수를 조회한다.")
    @GetMapping("/my/summary")
    public CustomResponse<ScrapResDTO.ScrapSummary> getScrapSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long userId = userDetails.getUser().getId();
        ScrapResDTO.ScrapSummary response = scrapQueryService.getScrapSummary(userId);
        
        return CustomResponse.onSuccess(response);
    }

    @Operation(summary = "공고 스크랩 추가", description = "로그인 사용자가 특정 공고를 스크랩한다.")
    @PostMapping("/{postingId}")
    public CustomResponse<String> addScrap(
            @PathVariable Long postingId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        scrapCommandService.addScrap(userId, postingId);

        return CustomResponse.onSuccess("공고 스크랩 성공");
    }

    @Operation(summary = "공고 스크랩 취소", description = "로그인 사용자의 특정 공고 스크랩을 삭제한다.")
    @DeleteMapping("/{postingId}")
    public CustomResponse<String> deleteScrap(
            @PathVariable Long postingId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        scrapCommandService.deleteScrap(userId, postingId);
        
        return CustomResponse.onSuccess("공고 스크랩 삭제 성공");
    }
}
