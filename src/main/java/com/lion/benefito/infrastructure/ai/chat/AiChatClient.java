package com.lion.benefito.infrastructure.ai.chat;

import java.util.List;

public interface AiChatClient {

    String generateReply(List<OpenAiChatMessage> messages);
}
