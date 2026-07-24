package com.lion.benefito.infrastructure.ai.chat;

import java.util.List;

public class MockAiChatClient implements AiChatClient {

    private static final String MOCK_REPLY = "현재 AI 연동 전 테스트 응답입니다.";

    @Override
    public String generateReply(List<OpenAiChatMessage> messages) {
        return MOCK_REPLY;
    }
}
