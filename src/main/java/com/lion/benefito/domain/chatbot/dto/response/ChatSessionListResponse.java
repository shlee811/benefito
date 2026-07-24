package com.lion.benefito.domain.chatbot.dto.response;

import com.lion.benefito.domain.chatbot.entity.ChatSession;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "대화 세션 목록 응답")
public record ChatSessionListResponse(
        @Schema(description = "세션 ID", example = "1") Long sessionId,
        @Schema(description = "세션 제목(첫 메시지 일부, 메시지가 없으면 \"새 대화\")", example = "장학금 추천해줘") String title,
        @Schema(description = "세션 생성일시") LocalDateTime createdAt,
        @Schema(description = "마지막 메시지 기준 갱신일시") LocalDateTime updatedAt
) {

    public static ChatSessionListResponse of(ChatSession chatSession, String title) {
        return new ChatSessionListResponse(
                chatSession.getId(),
                title,
                chatSession.getCreatedAt(),
                chatSession.getUpdatedAt()
        );
    }
}
