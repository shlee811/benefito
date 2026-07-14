package com.lion.benefito.domain.code.controller;

import com.lion.benefito.domain.code.dto.CommonCodeResponse;
import com.lion.benefito.domain.code.entity.CommonCodeType;
import com.lion.benefito.domain.code.service.CodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Common Code", description = "공통 코드 조회 API")
@RestController
@RequiredArgsConstructor
public class CodeController {

    private final CodeService codeService;

    @Operation(summary = "공통 코드 목록 조회", description = "code_type이 일치하고 is_active = true인 공통 코드를 display_order 오름차순으로 조회한다.")
    @GetMapping("/api/v1/common-codes")
    public ResponseEntity<List<CommonCodeResponse>> getCommonCodes(
            @Parameter(description = "조회할 코드 타입 (예: CATEGORY, REGION)", required = true)
            @RequestParam("type") String type) {
        CommonCodeType codeType = CommonCodeType.from(type);
        return ResponseEntity.ok(codeService.getCodesByType(codeType));
    }
}
