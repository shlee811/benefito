package com.lion.benefito.domain.todo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class TodoReqDTO {

    @Schema(description = "할 일 생성 요청 DTO")
    public record Create(
            @NotBlank(message = "할 일 내용을 입력해주세요.")
            @Schema(description = "할 일 내용", example = "자기소개서 작성")
            String content,

            @NotNull(message = "마감일을 입력해주세요.")
            @Schema(description = "마감일 (YYYY-MM-DD)", example = "2026-08-15")
            LocalDate endDate,

            @NotBlank(message = "카테고리 코드를 입력해주세요.")
            @Schema(description = "카테고리 영문 코드 (예: INTERN, SCHOLARSHIP 등)", example = "INTERN")
            String categoryCode
    ) {
    }

    @Schema(description = "할 일 수정 요청 DTO")
    public record Update(
            @NotBlank(message = "할 일 내용을 입력해주세요.")
            @Schema(description = "할 일 내용", example = "자기소개서 작성 완료하기")
            String content,

            @NotNull(message = "마감일을 입력해주세요.")
            @Schema(description = "마감일 (YYYY-MM-DD)", example = "2026-08-20")
            LocalDate endDate,

            @NotBlank(message = "카테고리 코드를 입력해주세요.")
            @Schema(description = "카테고리 영문 코드", example = "INTERN")
            String categoryCode
    ) {
    }
}
