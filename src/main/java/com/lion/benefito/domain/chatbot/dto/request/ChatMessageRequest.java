package com.lion.benefito.domain.chatbot.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅 메시지 요청")
public record ChatMessageRequest(
        @Schema(description = "사용자 메시지", example = "서울 지역 장학금을 추천해줘") String message,
        @Schema(description = "특정 공고에 대한 질문일 경우 공고 ID (선택)", example = "null", nullable = true) Long postingId
) {
}
