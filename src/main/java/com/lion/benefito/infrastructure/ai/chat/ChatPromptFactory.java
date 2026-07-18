package com.lion.benefito.infrastructure.ai.chat;

import com.lion.benefito.domain.posting.dto.PostingSimilarityResponse;
import java.util.List;
import org.springframework.stereotype.Component;

// DB 조회나 OpenAI API 호출 없이 프롬프트 문자열 조립만 담당한다.
@Component
public class ChatPromptFactory {

    private static final String SYSTEM_PROMPT = """
            당신은 Benefito 서비스의 AI 상담 어시스턴트입니다.
            대학생 및 청년을 위한 장학금, 정부지원사업, 채용, 인턴, 자격증, 공모전, 대외활동 공고를 추천합니다.

            다음 규칙을 반드시 지키세요.
            1. 아래 제공되는 "공고 목록" Context에 있는 정보만 근거로 답변하세요.
            2. Context에 없는 정보는 추측하지 마세요.
            3. 알 수 없는 내용은 모른다고 솔직하게 답하세요.
            4. 사용자의 질문 조건과 잘 맞는 공고를 우선적으로 추천하세요.
            5. 추천하는 각 공고마다 간단한 추천 이유를 설명하세요.
            6. 추천하는 공고의 원문 URL을 반드시 함께 제공하세요.
            7. Context에 사용자 질문과 관련된 공고가 없으면, 추측하지 말고 관련 공고를 찾지 못했다고 솔직하게 안내하세요.
            8. 한국어로 자연스럽고 간결하게 답변하세요.
            """;

    private static final String NO_RESULT_CONTEXT = "(검색 결과 없음)";
    private static final String UNKNOWN_VALUE = "정보 없음";

    public List<OpenAiChatMessage> createChatMessages(String question, List<PostingSimilarityResponse> searchResults) {
        if (question == null || question.isBlank()) {
            throw new ChatException(ChatErrorCode.INVALID_INPUT);
        }

        String userPrompt = buildUserPrompt(question, searchResults);
        return List.of(
                new OpenAiChatMessage("system", SYSTEM_PROMPT),
                new OpenAiChatMessage("user", userPrompt)
        );
    }

    private String buildUserPrompt(String question, List<PostingSimilarityResponse> searchResults) {
        return "[공고 목록]\n" + buildContext(searchResults) + "\n\n[사용자 질문]\n" + question;
    }

    private String buildContext(List<PostingSimilarityResponse> searchResults) {
        if (searchResults == null || searchResults.isEmpty()) {
            return NO_RESULT_CONTEXT;
        }

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < searchResults.size(); i++) {
            if (i > 0) {
                context.append("\n\n");
            }
            context.append(buildPostingBlock(i + 1, searchResults.get(i)));
        }
        return context.toString();
    }

    // 내부 DB ID 대신 [공고 N] 형태의 순번만 노출한다.
    private String buildPostingBlock(int number, PostingSimilarityResponse posting) {
        return "[공고 " + number + "]\n"
                + "제목: " + valueOrUnknown(posting.title()) + "\n"
                + "카테고리: " + valueOrUnknown(posting.category()) + "\n"
                + "내용: " + valueOrUnknown(posting.content()) + "\n"
                + "원문 URL: " + valueOrUnknown(posting.sourceUrl());
    }

    private String valueOrUnknown(String value) {
        return (value == null || value.isBlank()) ? UNKNOWN_VALUE : value;
    }
}
