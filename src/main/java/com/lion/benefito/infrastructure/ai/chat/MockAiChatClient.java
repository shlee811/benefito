package com.lion.benefito.infrastructure.ai.chat;

import org.springframework.stereotype.Component;

@Component
public class MockAiChatClient implements AiChatClient {

    private static final String MOCK_REPLY = "현재 AI 연동 전 테스트 응답입니다.";

    @Override
    public String generateReply(String message, Long postingId) {
        return MOCK_REPLY;
    }
}
