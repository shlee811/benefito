package com.lion.benefito.infrastructure.ai.chat;

import java.util.List;

public record OpenAiChatResponse(List<Choice> choices) {

    public record Choice(OpenAiChatMessage message) {
    }
}
