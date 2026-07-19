package com.lion.benefito.infrastructure.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lion.benefito.domain.posting.dto.PostingSimilarityResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatPromptFactoryTest {

    private final ChatPromptFactory chatPromptFactory = new ChatPromptFactory();

    @Test
    void 검색결과가_있으면_시스템_사용자_메시지_2개를_반환하고_공고_정보가_포함된다() {
        PostingSimilarityResponse posting1 = new PostingSimilarityResponse(
                4L, "핀테크 블록체인 개발 인턴십", "스마트컨트랙트 개발 인턴 모집", "INTERN",
                "https://example.com/rag/4", 0.9);
        PostingSimilarityResponse posting2 = new PostingSimilarityResponse(
                3L, "반도체 공정 품질관리(QC) 엔지니어 채용", "웨이퍼 검사 및 수율 개선 업무", "JOB",
                "https://example.com/rag/3", 0.5);

        List<OpenAiChatMessage> messages = chatPromptFactory.createChatMessages(
                "블록체인 관련 인턴 자리를 찾고 있어", List.of(posting1, posting2));

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).role()).isEqualTo("system");
        assertThat(messages.get(1).role()).isEqualTo("user");

        String userPrompt = messages.get(1).content();
        assertThat(userPrompt).contains("[공고 1]", "[공고 2]");
        assertThat(userPrompt).contains("핀테크 블록체인 개발 인턴십", "반도체 공정 품질관리(QC) 엔지니어 채용");
        assertThat(userPrompt).contains("https://example.com/rag/4", "https://example.com/rag/3");
        assertThat(userPrompt).contains("블록체인 관련 인턴 자리를 찾고 있어");
        // 내부 DB ID(4, 3)는 노출되지 않고 순번([공고 1], [공고 2])만 노출되어야 한다.
        assertThat(userPrompt).doesNotContain("공고 ID");
    }

    @Test
    void 검색결과가_null이면_검색_결과_없음으로_처리된다() {
        List<OpenAiChatMessage> messages = chatPromptFactory.createChatMessages("아무 질문", null);

        assertThat(messages.get(1).content()).contains("(검색 결과 없음)");
    }

    @Test
    void 검색결과가_빈_리스트여도_예외없이_검색_결과_없음으로_처리된다() {
        List<OpenAiChatMessage> messages = chatPromptFactory.createChatMessages("아무 질문", List.of());

        assertThat(messages.get(1).content()).contains("(검색 결과 없음)");
    }

    @Test
    void 질문이_null이면_예외() {
        assertThatThrownBy(() -> chatPromptFactory.createChatMessages(null, List.of()))
                .isInstanceOf(ChatException.class)
                .satisfies(ex -> assertThat(((ChatException) ex).getCode())
                        .isEqualTo(ChatErrorCode.INVALID_INPUT));
    }

    @Test
    void 질문이_blank면_예외() {
        assertThatThrownBy(() -> chatPromptFactory.createChatMessages("   ", List.of()))
                .isInstanceOf(ChatException.class)
                .satisfies(ex -> assertThat(((ChatException) ex).getCode())
                        .isEqualTo(ChatErrorCode.INVALID_INPUT));
    }

    @Test
    void 공고_필드가_null이어도_NPE없이_정보없음으로_처리된다() {
        PostingSimilarityResponse postingWithNulls = new PostingSimilarityResponse(
                1L, null, null, null, null, 0.1);

        List<OpenAiChatMessage> messages = chatPromptFactory.createChatMessages(
                "질문", List.of(postingWithNulls));

        String userPrompt = messages.get(1).content();
        assertThat(userPrompt).contains("[공고 1]");
        assertThat(userPrompt).contains("정보 없음");
    }
}
