package com.lion.benefito.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lion.benefito.domain.chatbot.dto.request.ChatMessageRequest;
import com.lion.benefito.domain.chatbot.dto.response.ChatMessageResponse;
import com.lion.benefito.domain.chatbot.entity.ChatMessage;
import com.lion.benefito.domain.chatbot.entity.ChatRole;
import com.lion.benefito.domain.chatbot.entity.ChatSession;
import com.lion.benefito.domain.chatbot.repository.ChatMessageRepository;
import com.lion.benefito.domain.chatbot.repository.ChatSessionRepository;
import com.lion.benefito.domain.posting.dto.PostingSimilarityResponse;
import com.lion.benefito.domain.user.entity.User;
import com.lion.benefito.domain.user.repository.UserRepository;
import com.lion.benefito.infrastructure.ai.chat.AiChatClient;
import com.lion.benefito.infrastructure.ai.chat.ChatPromptFactory;
import com.lion.benefito.infrastructure.ai.chat.OpenAiChatMessage;
import com.lion.benefito.infrastructure.ai.rag.PostingRetriever;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    private static final int TOP_K = 5;

    @Mock
    private ChatSessionRepository chatSessionRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AiChatClient aiChatClient;
    @Mock
    private PostingRetriever postingRetriever;
    @Mock
    private ChatPromptFactory chatPromptFactory;

    private ChatbotService chatbotService;

    private ChatSession chatSession;
    private User user;

    @BeforeEach
    void setUp() {
        chatbotService = new ChatbotService(
                chatSessionRepository, chatMessageRepository, userRepository, aiChatClient,
                postingRetriever, chatPromptFactory);

        user = User.builder().build();
        setId(user, 1L);
        chatSession = ChatSession.builder().user(user).build();
        setId(chatSession, 10L);
    }

    @Test
    void sendMessage는_RAG_검색_프롬프트_조립_AI_호출_순서로_동작하고_메시지를_저장한다() {
        ChatMessageRequest request = new ChatMessageRequest("서울 지역 장학금을 추천해줘", null);
        List<PostingSimilarityResponse> searchResults = List.of(
                new PostingSimilarityResponse(1L, "국가장학금", "내용", "SCHOLARSHIP", "https://example.com/1", 0.9));
        List<OpenAiChatMessage> promptMessages = List.of(
                new OpenAiChatMessage("system", "sys"), new OpenAiChatMessage("user", "user"));

        when(chatSessionRepository.findById(10L)).thenReturn(Optional.of(chatSession));
        when(postingRetriever.findSimilarPostings("서울 지역 장학금을 추천해줘", TOP_K)).thenReturn(searchResults);
        when(chatPromptFactory.createChatMessages("서울 지역 장학금을 추천해줘", searchResults)).thenReturn(promptMessages);
        when(aiChatClient.generateReply(promptMessages)).thenReturn("장학금 추천 답변입니다.");

        ChatMessageResponse response = chatbotService.sendMessage(1L, 10L, request);

        assertThat(response.message()).isEqualTo("장학금 추천 답변입니다.");
        assertThat(response.role()).isEqualTo(ChatRole.ASSISTANT);

        verify(postingRetriever).findSimilarPostings("서울 지역 장학금을 추천해줘", TOP_K);
        verify(chatPromptFactory).createChatMessages("서울 지역 장학금을 추천해줘", searchResults);
        verify(aiChatClient).generateReply(promptMessages);

        ArgumentCaptor<ChatMessage> savedMessages = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, org.mockito.Mockito.times(2)).save(savedMessages.capture());
        List<ChatMessage> captured = savedMessages.getAllValues();
        assertThat(captured.get(0).getRole()).isEqualTo(ChatRole.USER);
        assertThat(captured.get(0).getMessage()).isEqualTo("서울 지역 장학금을 추천해줘");
        assertThat(captured.get(1).getRole()).isEqualTo(ChatRole.ASSISTANT);
        assertThat(captured.get(1).getMessage()).isEqualTo("장학금 추천 답변입니다.");
    }

    @Test
    void EmbeddingClient를_별도로_호출하지_않는다() {
        ChatMessageRequest request = new ChatMessageRequest("질문", null);
        when(chatSessionRepository.findById(10L)).thenReturn(Optional.of(chatSession));
        when(postingRetriever.findSimilarPostings(anyString(), anyInt())).thenReturn(List.of());
        when(chatPromptFactory.createChatMessages(anyString(), anyList())).thenReturn(List.of());
        when(aiChatClient.generateReply(anyList())).thenReturn("답변");

        chatbotService.sendMessage(1L, 10L, request);

        // ChatbotService는 postingRetriever/chatPromptFactory/aiChatClient만 알고 있고,
        // EmbeddingClient에 대한 의존성 자체가 없다 (생성자에 주입되지 않음) — 컴파일 시점에 이미 보장됨.
        verify(postingRetriever).findSimilarPostings(eq("질문"), eq(TOP_K));
    }

    @Test
    void 검색_결과가_빈_리스트여도_정상적으로_답변한다() {
        ChatMessageRequest request = new ChatMessageRequest("아무도 없는 주제", null);
        when(chatSessionRepository.findById(10L)).thenReturn(Optional.of(chatSession));
        when(postingRetriever.findSimilarPostings("아무도 없는 주제", TOP_K)).thenReturn(List.of());
        when(chatPromptFactory.createChatMessages(eq("아무도 없는 주제"), anyList()))
                .thenReturn(List.of(new OpenAiChatMessage("system", "sys"), new OpenAiChatMessage("user", "u")));
        when(aiChatClient.generateReply(anyList())).thenReturn("관련 공고를 찾지 못했습니다.");

        ChatMessageResponse response = chatbotService.sendMessage(1L, 10L, request);

        assertThat(response.message()).isEqualTo("관련 공고를 찾지 못했습니다.");
    }

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
