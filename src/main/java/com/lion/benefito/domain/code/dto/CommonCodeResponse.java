package com.lion.benefito.domain.code.dto;

import com.lion.benefito.domain.code.entity.Code;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공통 코드 응답")
public record CommonCodeResponse(
        @Schema(description = "코드 ID", example = "1") Long id,
        @Schema(description = "코드 값", example = "SCHOLARSHIP") String code,
        @Schema(description = "코드 명", example = "장학금") String name,
        @Schema(description = "정렬 순서", example = "1") Integer displayOrder
) {

    public static CommonCodeResponse from(Code code) {
        return new CommonCodeResponse(
                code.getId(),
                code.getCode(),
                code.getName(),
                code.getDisplayOrder()
        );
    }
}
