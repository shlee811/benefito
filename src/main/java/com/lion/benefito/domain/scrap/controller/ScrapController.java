package com.lion.benefito.domain.scrap.controller;

import com.lion.benefito.domain.scrap.service.ScrapService;
import com.lion.benefito.global.common.CustomResponse;
import com.lion.benefito.global.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Scrap", description = "공고 스크랩 API")
@RestController
@RequiredArgsConstructor
public class ScrapController {

    private final ScrapService scrapService;
    private final CurrentUserProvider currentUserProvider;

    @Operation(summary = "공고 스크랩 취소", description = "로그인 사용자의 특정 공고 스크랩을 삭제한다.")
    @DeleteMapping("/api/v1/scraps/{postingId}")
    public ResponseEntity<CustomResponse<Void>> deleteScrap(@PathVariable Long postingId) {
        Long userId = currentUserProvider.getCurrentUserId();
        scrapService.deleteScrap(userId, postingId);
        CustomResponse<Void> response = CustomResponse.onSuccess(null);
        return ResponseEntity.ok(response);
    }
}
