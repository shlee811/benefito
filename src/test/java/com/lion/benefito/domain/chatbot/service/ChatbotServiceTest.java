package com.lion.benefito.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lion.benefito.domain.chatbot.dto.request.ChatMessageRequest;
import com.lion.benefito.domain.chatbot.dto.response.ChatMessageResponse;
import com.lion.benefito.domain.chatbot.dto.response.ChatSessionCreateResponse;
import com.lion.benefito.domain.chatbot.entity.ChatMessage;
import com.lion.benefito.domain.chatbot.entity.ChatRole;
import com.lion.benefito.domain.chatbot.entity.ChatSession;
import com.lion.benefito.domain.chatbot.exception.ChatbotErrorCode;
import com.lion.benefito.domain.chatbot.exception.ChatbotException;
import com.lion.benefito.domain.chatbot.repository.ChatMessageRepository;
import com.lion.benefito.domain.chatbot.repository.ChatSessionRepository;
import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.posting.dto.PostingSimilarityResponse;
import com.lion.benefito.domain.user.entity.User;
import com.lion.benefito.domain.user.entity.UserInterest;
import com.lion.benefito.domain.user.enums.Grade;
import com.lion.benefito.domain.user.enums.Income;
import com.lion.benefito.domain.user.repository.UserInterestRepository;
import com.lion.benefito.domain.user.repository.UserRepository;
import com.lion.benefito.infrastructure.ai.chat.AiChatClient;
import com.lion.benefito.infrastructure.ai.chat.ChatPromptFactory;
import com.lion.benefito.infrastructure.ai.chat.OpenAiChatMessage;
import com.lion.benefito.infrastructure.ai.chat.UserProfileContext;
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
    private UserInterestRepository userInterestRepository;
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
                chatSessionRepository, chatMessageRepository, userRepository, userInterestRepository, aiChatClient,
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
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(chatPromptFactory.createChatMessages(
                eq("서울 지역 장학금을 추천해줘"), eq(searchResults), any(UserProfileContext.class)))
                .thenReturn(promptMessages);
        when(aiChatClient.generateReply(promptMessages)).thenReturn("장학금 추천 답변입니다.");

        ChatMessageResponse response = chatbotService.sendMessage(1L, 10L, request);

        assertThat(response.message()).isEqualTo("장학금 추천 답변입니다.");
        assertThat(response.role()).isEqualTo(ChatRole.ASSISTANT);

        verify(postingRetriever).findSimilarPostings("서울 지역 장학금을 추천해줘", TOP_K);
        verify(chatPromptFactory).createChatMessages(
                eq("서울 지역 장학금을 추천해줘"), eq(searchResults), any(UserProfileContext.class));
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
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(chatPromptFactory.createChatMessages(anyString(), anyList(), any())).thenReturn(List.of());
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
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(chatPromptFactory.createChatMessages(eq("아무도 없는 주제"), anyList(), any()))
                .thenReturn(List.of(new OpenAiChatMessage("system", "sys"), new OpenAiChatMessage("user", "u")));
        when(aiChatClient.generateReply(anyList())).thenReturn("관련 공고를 찾지 못했습니다.");

        ChatMessageResponse response = chatbotService.sendMessage(1L, 10L, request);

        assertThat(response.message()).isEqualTo("관련 공고를 찾지 못했습니다.");
    }

    @Test
    void 관심사와_프로필_정보가_있는_사용자는_사용자_정보가_프롬프트_조립에_전달된다() {
        Code regionCode = mock(Code.class);
        when(regionCode.getName()).thenReturn("서울");
        Code categoryCode1 = mock(Code.class);
        when(categoryCode1.getName()).thenReturn("장학금");
        Code categoryCode2 = mock(Code.class);
        when(categoryCode2.getName()).thenReturn("정부지원사업");

        User profiledUser = User.builder()
                .grade(Grade.GRADE_4)
                .income(Income.INCOME_3)
                .regionCode(regionCode)
                .build();
        setId(profiledUser, 1L);
        ChatSession session = ChatSession.builder().user(profiledUser).build();
        setId(session, 10L);

        UserInterest interest1 = UserInterest.builder().user(profiledUser).categoryCode(categoryCode1).build();
        UserInterest interest2 = UserInterest.builder().user(profiledUser).categoryCode(categoryCode2).build();

        ChatMessageRequest request = new ChatMessageRequest("장학금 추천해줘", null);
        List<PostingSimilarityResponse> searchResults = List.of();
        List<OpenAiChatMessage> promptMessages = List.of(
                new OpenAiChatMessage("system", "sys"), new OpenAiChatMessage("user", "user"));

        when(chatSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(postingRetriever.findSimilarPostings("장학금 추천해줘", TOP_K)).thenReturn(searchResults);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(profiledUser));
        when(userInterestRepository.findByUserId(1L)).thenReturn(List.of(interest1, interest2));
        when(chatPromptFactory.createChatMessages(anyString(), anyList(), any(UserProfileContext.class)))
                .thenReturn(promptMessages);
        when(aiChatClient.generateReply(promptMessages)).thenReturn("답변");

        chatbotService.sendMessage(1L, 10L, request);

        ArgumentCaptor<UserProfileContext> profileCaptor = ArgumentCaptor.forClass(UserProfileContext.class);
        verify(chatPromptFactory).createChatMessages(eq("장학금 추천해줘"), eq(searchResults), profileCaptor.capture());
        UserProfileContext captured = profileCaptor.getValue();
        assertThat(captured.grade()).isEqualTo("4학년");
        assertThat(captured.income()).isEqualTo("3분위");
        assertThat(captured.regionName()).isEqualTo("서울");
        assertThat(captured.categoryNames()).containsExactly("장학금", "정부지원사업");
    }

    @Test
    void 관심사가_없는_사용자도_챗봇_기능이_정상적으로_동작한다() {
        ChatMessageRequest request = new ChatMessageRequest("질문", null);
        List<OpenAiChatMessage> promptMessages = List.of(
                new OpenAiChatMessage("system", "sys"), new OpenAiChatMessage("user", "user"));

        when(chatSessionRepository.findById(10L)).thenReturn(Optional.of(chatSession));
        when(postingRetriever.findSimilarPostings("질문", TOP_K)).thenReturn(List.of());
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(userInterestRepository.findByUserId(1L)).thenReturn(List.of());
        when(chatPromptFactory.createChatMessages(anyString(), anyList(), any(UserProfileContext.class)))
                .thenReturn(promptMessages);
        when(aiChatClient.generateReply(promptMessages)).thenReturn("답변");

        ChatMessageResponse response = chatbotService.sendMessage(1L, 10L, request);

        assertThat(response.message()).isEqualTo("답변");

        ArgumentCaptor<UserProfileContext> profileCaptor = ArgumentCaptor.forClass(UserProfileContext.class);
        verify(chatPromptFactory).createChatMessages(eq("질문"), anyList(), profileCaptor.capture());
        assertThat(profileCaptor.getValue().categoryNames()).isEmpty();
    }

    @Test
    void createSession은_전달받은_실제_userId로_User를_조회해_세션에_저장한다() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ChatSessionCreateResponse response = chatbotService.createSession(1L);

        ArgumentCaptor<ChatSession> savedSession = ArgumentCaptor.forClass(ChatSession.class);
        verify(chatSessionRepository).save(savedSession.capture());
        assertThat(savedSession.getValue().getUser().getId()).isEqualTo(1L);
        assertThat(response).isNotNull();
    }

    @Test
    void 사용자_A가_사용자_B_소유_세션에_메시지를_보내면_접근이_차단된다() {
        Long ownerUserId = 1L;
        Long otherUserId = 2L;
        ChatMessageRequest request = new ChatMessageRequest("질문", null);

        when(chatSessionRepository.findById(10L)).thenReturn(Optional.of(chatSession));

        assertThatThrownBy(() -> chatbotService.sendMessage(otherUserId, 10L, request))
                .isInstanceOf(ChatbotException.class)
                .satisfies(ex -> assertThat(((ChatbotException) ex).getCode())
                        .isEqualTo(ChatbotErrorCode.SESSION_ACCESS_DENIED));

        verify(chatMessageRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 사용자_A가_사용자_B_소유_세션의_메시지를_조회하면_접근이_차단된다() {
        Long otherUserId = 2L;
        when(chatSessionRepository.findById(10L)).thenReturn(Optional.of(chatSession));

        assertThatThrownBy(() -> chatbotService.getMessages(otherUserId, 10L))
                .isInstanceOf(ChatbotException.class)
                .satisfies(ex -> assertThat(((ChatbotException) ex).getCode())
                        .isEqualTo(ChatbotErrorCode.SESSION_ACCESS_DENIED));

        verify(chatMessageRepository, never()).findByChatSessionIdOrderByCreatedAtAscIdAsc(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 소유자_본인이면_세션에_정상적으로_접근할_수_있다() {
        when(chatSessionRepository.findById(10L)).thenReturn(Optional.of(chatSession));
        when(chatMessageRepository.findByChatSessionIdOrderByCreatedAtAscIdAsc(10L)).thenReturn(List.of());

        List<ChatMessageResponse> messages = chatbotService.getMessages(1L, 10L);

        assertThat(messages).isEmpty();
    }

    @Test
    void 존재하지_않는_세션에_접근하면_SESSION_NOT_FOUND_예외() {
        when(chatSessionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatbotService.getMessages(1L, 999L))
                .isInstanceOf(ChatbotException.class)
                .satisfies(ex -> assertThat(((ChatbotException) ex).getCode())
                        .isEqualTo(ChatbotErrorCode.SESSION_NOT_FOUND));
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
