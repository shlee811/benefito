package com.lion.benefito.domain.chatbot.dto.response;

import com.lion.benefito.domain.chatbot.entity.ChatMessage;
import com.lion.benefito.domain.chatbot.entity.ChatRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "채팅 메시지 응답")
public record ChatMessageResponse(
        @Schema(description = "메시지 ID") Long id,
        @Schema(description = "메시지 역할 (USER, ASSISTANT)") ChatRole role,
        @Schema(description = "메시지 내용") String message,
        @Schema(description = "이 답변의 RAG 검색 결과 중 답변 본문에서 실제로 이름이 언급된 추천 공고 카드 목록. "
                + "USER 메시지이거나 검색 결과가 없거나 답변에서 언급되지 않았으면 빈 배열이다.")
        List<PostingCardResponse> postings,
        @Schema(description = "생성일시") LocalDateTime createdAt
) {

    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return from(chatMessage, List.of());
    }

    public static ChatMessageResponse from(ChatMessage chatMessage, List<PostingCardResponse> postings) {
        return new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getRole(),
                chatMessage.getMessage(),
                postings,
                chatMessage.getCreatedAt()
        );
    }
}
