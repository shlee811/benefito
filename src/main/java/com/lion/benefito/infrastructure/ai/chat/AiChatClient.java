package com.lion.benefito.infrastructure.ai.chat;

public interface AiChatClient {

    String generateReply(String message, Long postingId);
}
