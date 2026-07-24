package com.lion.benefito.domain.chatbot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대화 세션 생성 응답")
public record ChatSessionCreateResponse(
        @Schema(description = "생성된 세션 ID", example = "1") Long sessionId
) {
}
