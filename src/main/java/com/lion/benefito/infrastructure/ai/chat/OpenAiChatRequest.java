package com.lion.benefito.infrastructure.ai.chat;

import java.util.List;

public record OpenAiChatRequest(String model, List<OpenAiChatMessage> messages) {
}
