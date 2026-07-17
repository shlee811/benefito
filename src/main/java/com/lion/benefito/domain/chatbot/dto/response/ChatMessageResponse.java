package com.lion.benefito.domain.chatbot.dto.response;

import com.lion.benefito.domain.chatbot.entity.ChatMessage;
import com.lion.benefito.domain.chatbot.entity.ChatRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "채팅 메시지 응답")
public record ChatMessageResponse(
        @Schema(description = "메시지 ID") Long id,
        @Schema(description = "메시지 역할 (USER, ASSISTANT)") ChatRole role,
        @Schema(description = "메시지 내용") String message,
        @Schema(description = "생성일시") LocalDateTime createdAt
) {

    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getRole(),
                chatMessage.getMessage(),
                chatMessage.getCreatedAt()
        );
    }
}
