package com.lion.benefito.infrastructure.ai.chat;

import java.util.List;

// ChatbotService가 조회한 사용자 정보를 ChatPromptFactory로 전달하기 위한 내부 운반용 객체다.
public record UserProfileContext(
        String grade,
        String income,
        String regionName,
        List<String> categoryNames
) {
}
