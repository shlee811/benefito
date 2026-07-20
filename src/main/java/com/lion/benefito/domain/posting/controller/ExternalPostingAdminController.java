package com.lion.benefito.domain.posting.controller;

import com.lion.benefito.domain.posting.dto.ExternalPostingImportResponse;
import com.lion.benefito.domain.posting.service.ExternalPostingIngestService;
import com.lion.benefito.global.common.CustomResponse;
import com.lion.benefito.infrastructure.external.kstartup.KStartupPostingSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 인증이 아직 없어 external.admin-api.enabled=true일 때만 빈이 등록되는 개발 전용 API.
 * 값을 켜지 않으면 이 컨트롤러 자체가 존재하지 않아 엔드포인트가 404가 된다.
 */
@Tag(name = "External Posting Admin", description = "외부 공고 수집 관리자 API (개발 환경 전용)")
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "external.admin-api", name = "enabled", havingValue = "true")
public class ExternalPostingAdminController {

    private final ExternalPostingIngestService externalPostingIngestService;
    private final KStartupPostingSource kStartupPostingSource;

    @Operation(summary = "K-Startup 공고 수동 수집", description = "K-Startup 공고를 즉시 수집해 저장/갱신하고 임베딩을 재생성한다.")
    @PostMapping("/api/v1/admin/external-postings/kstartup/import")
    public ResponseEntity<CustomResponse<ExternalPostingImportResponse>> importKStartupPostings() {
        ExternalPostingImportResponse response = externalPostingIngestService.importFrom(kStartupPostingSource);
        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }
}
