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
import com.lion.benefito.domain.chatbot.dto.response.PostingCardResponse;
import com.lion.benefito.domain.chatbot.entity.ChatMessage;
import com.lion.benefito.domain.chatbot.entity.ChatMessagePosting;
import com.lion.benefito.domain.chatbot.entity.ChatRole;
import com.lion.benefito.domain.chatbot.entity.ChatSession;
import com.lion.benefito.domain.chatbot.exception.ChatbotErrorCode;
import com.lion.benefito.domain.chatbot.exception.ChatbotException;
import com.lion.benefito.domain.chatbot.repository.ChatMessagePostingRepository;
import com.lion.benefito.domain.chatbot.repository.ChatMessageRepository;
import com.lion.benefito.domain.chatbot.repository.ChatSessionRepository;
import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.posting.dto.PostingSimilarityResponse;
import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.domain.posting.repository.PostingRepository;
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
    @Mock
    private PostingRepository postingRepository;
    @Mock
    private ChatMessagePostingRepository chatMessagePostingRepository;

    private ChatbotService chatbotService;

    private ChatSession chatSession;
    private User user;

    @BeforeEach
    void setUp() {
        chatbotService = new ChatbotService(
                chatSessionRepository, chatMessageRepository, userRepository, userInterestRepository, aiChatClient,
                postingRetriever, chatPromptFactory, postingRepository, chatMessagePostingRepository);

        user = User.builder().build();
        setId(user, 1L);
        chatSession = ChatSession.builder().user(user).build();
        setId(chatSession, 10L);
    }

    @Test
    void sendMessage는_RAG_검색_프롬프트_조립_AI_호출_순서로_동작하고_메시지를_저장한다() {
        ChatMessageRequest request = new ChatMessageRequest("서울 지역 장학금을 추천해줘", null);
        List<PostingSimilarityResponse> searchResults = List.of(
                new PostingSimilarityResponse(1L, "국가장학금", "내용", "SCHOLARSHIP", "정부", "SEOUL",
                        java.time.LocalDate.of(2026, 8, 31), "https://example.com/1", 0.9));
        List<OpenAiChatMessage> promptMessages = List.of(
                new OpenAiChatMessage("system", "sys"), new OpenAiChatMessage("user", "user"));

        when(chatSessionRepository.findById(10L)).thenReturn(Optional.of(chatSession));
        when(postingRetriever.findSimilarPostings("서울 지역 장학금을 추천해줘", TOP_K)).thenReturn(searchResults);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(chatPromptFactory.createChatMessages(
                eq("서울 지역 장학금을 추천해줘"), eq(searchResults), any(UserProfileContext.class)))
                .thenReturn(promptMessages);
        when(aiChatClient.generateReply(promptMessages)).thenReturn("국가장학금을 추천드립니다. 신청해보세요.");
        Posting posting = mock(Posting.class);
        when(postingRepository.getReferenceById(1L)).thenReturn(posting);

        ChatMessageResponse response = chatbotService.sendMessage(1L, 10L, request);

        assertThat(response.message()).isEqualTo("국가장학금을 추천드립니다. 신청해보세요.");
        assertThat(response.role()).isEqualTo(ChatRole.ASSISTANT);

        // 답변 아래 카드는 프롬프트 조립에 쓴 것과 동일한 searchResults 중, 답변 본문에서 실제로
        // 이름이 언급된 것만 남아야 한다(별도 조회 결과가 아니라 같은 RAG 검색 결과 안에서만 고른 것).
        assertThat(response.postings()).hasSize(1);
        PostingCardResponse card = response.postings().get(0);
        assertThat(card.postingId()).isEqualTo(1L);
        assertThat(card.title()).isEqualTo("국가장학금");
        assertThat(card.organization()).isEqualTo("정부");
        assertThat(card.categoryCode()).isEqualTo("SCHOLARSHIP");
        assertThat(card.region()).isEqualTo("SEOUL");
        assertThat(card.deadline()).isEqualTo(java.time.LocalDate.of(2026, 8, 31));
        assertThat(card.link()).isEqualTo("https://example.com/1");

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
        assertThat(captured.get(1).getMessage()).isEqualTo("국가장학금을 추천드립니다. 신청해보세요.");

        // 답변 본문에서 언급된 공고만 DB에도 저장되어야 getMessages()로 다시 조회할 때도
        // 같은 카드가 복원된다.
        ArgumentCaptor<List<ChatMessagePosting>> savedPostingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(chatMessagePostingRepository).saveAll(savedPostingsCaptor.capture());
        List<ChatMessagePosting> savedChatMessagePostings = savedPostingsCaptor.getValue();
        assertThat(savedChatMessagePostings).hasSize(1);
        assertThat(savedChatMessagePostings.get(0).getPosting()).isEqualTo(posting);
        assertThat(savedChatMessagePostings.get(0).getDisplayOrder()).isEqualTo(0);
        assertThat(savedChatMessagePostings.get(0).getChatMessage()).isEqualTo(captured.get(1));
    }

    @Test
    void 서로_다른_질문은_서로_다른_RAG_검색_결과가_그대로_카드로_반환된다() {
        List<OpenAiChatMessage> promptMessages = List.of(new OpenAiChatMessage("system", "sys"));

        ChatMessageRequest itRequest = new ChatMessageRequest("IT 공모전 추천해줘", null);
        List<PostingSimilarityResponse> itResults = List.of(
                new PostingSimilarityResponse(101L, "전국 대학생 빅데이터 분석 공모전", "내용", "CONTEST", "라이언데이터", "전국",
                        java.time.LocalDate.of(2026, 9, 1), "https://example.com/101", 0.95));
        when(chatSessionRepository.findById(10L)).thenReturn(Optional.of(chatSession));
        when(postingRetriever.findSimilarPostings("IT 공모전 추천해줘", TOP_K)).thenReturn(itResults);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(chatPromptFactory.createChatMessages(eq("IT 공모전 추천해줘"), eq(itResults), any()))
                .thenReturn(promptMessages);
        when(aiChatClient.generateReply(promptMessages)).thenReturn("전국 대학생 빅데이터 분석 공모전을 추천드립니다.");
        when(postingRepository.getReferenceById(101L)).thenReturn(mock(Posting.class));

        ChatMessageResponse itResponse = chatbotService.sendMessage(1L, 10L, itRequest);

        ChatMessageRequest envRequest = new ChatMessageRequest("환경 공모전 추천해줘", null);
        List<PostingSimilarityResponse> envResults = List.of(
                new PostingSimilarityResponse(202L, "탄소중립 아이디어 공모전", "내용", "CONTEST", "환경부", "전국",
                        java.time.LocalDate.of(2026, 10, 1), "https://example.com/202", 0.9));
        when(postingRetriever.findSimilarPostings("환경 공모전 추천해줘", TOP_K)).thenReturn(envResults);
        when(chatPromptFactory.createChatMessages(eq("환경 공모전 추천해줘"), eq(envResults), any()))
                .thenReturn(promptMessages);
        when(aiChatClient.generateReply(promptMessages)).thenReturn("탄소중립 아이디어 공모전을 추천드립니다.");
        when(postingRepository.getReferenceById(202L)).thenReturn(mock(Posting.class));

        ChatMessageResponse envResponse = chatbotService.sendMessage(1L, 10L, envRequest);

        assertThat(itResponse.postings()).extracting(PostingCardResponse::postingId).containsExactly(101L);
        assertThat(envResponse.postings()).extracting(PostingCardResponse::postingId).containsExactly(202L);
        // 질문이 바뀌면 이전 질문의 카드가 이어서 남아있지 않고 새 응답으로 완전히 교체되어야 한다.
        assertThat(envResponse.postings()).extracting(PostingCardResponse::postingId).doesNotContain(101L);
    }

    @Test
    void RAG로_검색됐지만_답변_본문에서_이름이_언급되지_않은_공고는_카드로_표시되지_않는다() {
        ChatMessageRequest request = new ChatMessageRequest("공모전 추천해줘", null);
        List<PostingSimilarityResponse> searchResults = List.of(
                new PostingSimilarityResponse(1L, "언급된 공모전", "내용", "CONTEST", "주최사A", "전국", null,
                        "https://example.com/1", 0.9),
                new PostingSimilarityResponse(2L, "언급되지 않은 공모전", "내용", "CONTEST", "주최사B", "전국", null,
                        "https://example.com/2", 0.8));
        List<OpenAiChatMessage> promptMessages = List.of(new OpenAiChatMessage("system", "sys"));

        when(chatSessionRepository.findById(10L)).thenReturn(Optional.of(chatSession));
        when(postingRetriever.findSimilarPostings("공모전 추천해줘", TOP_K)).thenReturn(searchResults);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(chatPromptFactory.createChatMessages(eq("공모전 추천해줘"), eq(searchResults), any()))
                .thenReturn(promptMessages);
        when(aiChatClient.generateReply(promptMessages)).thenReturn("언급된 공모전을 추천드립니다.");
        when(postingRepository.getReferenceById(1L)).thenReturn(mock(Posting.class));

        ChatMessageResponse response = chatbotService.sendMessage(1L, 10L, request);

        assertThat(response.postings()).extracting(PostingCardResponse::title).containsExactly("언급된 공모전");
        verify(postingRepository, never()).getReferenceById(2L);

        ArgumentCaptor<List<ChatMessagePosting>> savedPostingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(chatMessagePostingRepository).saveAll(savedPostingsCaptor.capture());
        assertThat(savedPostingsCaptor.getValue()).hasSize(1);
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
        // RAG 결과가 없으면 카드도 빈 배열이어야 한다(고정 카테고리 조회 등으로 대체되면 안 됨).
        assertThat(response.postings()).isEmpty();
        verify(postingRepository, never()).getReferenceById(org.mockito.ArgumentMatchers.any());
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
    void 메시지를_보내면_세션의_messageCount가_증가하고_세션이_다시_저장된다() {
        ChatMessageRequest request = new ChatMessageRequest("질문", null);
        when(chatSessionRepository.findById(10L)).thenReturn(Optional.of(chatSession));
        when(postingRetriever.findSimilarPostings(anyString(), anyInt())).thenReturn(List.of());
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(chatPromptFactory.createChatMessages(anyString(), anyList(), any())).thenReturn(List.of());
        when(aiChatClient.generateReply(anyList())).thenReturn("답변");

        chatbotService.sendMessage(1L, 10L, request);

        // 사용자 메시지 저장 후 1번, 어시스턴트 메시지 저장 후 1번 = 총 2번 touch되어 messageCount는 2가 된다.
        verify(chatSessionRepository, org.mockito.Mockito.times(2)).save(chatSession);
        assertThat(chatSession.getMessageCount()).isEqualTo(2);
    }

    @Test
    void AI_호출이_실패해도_이미_저장된_사용자_메시지는_유지되고_어시스턴트_메시지는_저장되지_않는다() {
        ChatMessageRequest request = new ChatMessageRequest("질문", null);
        when(chatSessionRepository.findById(10L)).thenReturn(Optional.of(chatSession));
        when(postingRetriever.findSimilarPostings(anyString(), anyInt())).thenReturn(List.of());
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(chatPromptFactory.createChatMessages(anyString(), anyList(), any())).thenReturn(List.of());
        when(aiChatClient.generateReply(anyList())).thenThrow(new RuntimeException("OpenAI 호출 실패"));

        assertThatThrownBy(() -> chatbotService.sendMessage(1L, 10L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("OpenAI 호출 실패");

        ArgumentCaptor<ChatMessage> savedMessages = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, org.mockito.Mockito.times(1)).save(savedMessages.capture());
        assertThat(savedMessages.getValue().getRole()).isEqualTo(ChatRole.USER);
        assertThat(savedMessages.getValue().getMessage()).isEqualTo("질문");
        // 사용자 메시지 저장 시점의 touch까지는 일어났어야 한다(그 뒤 AI 호출에서 실패).
        verify(chatSessionRepository, org.mockito.Mockito.times(1)).save(chatSession);
    }

    @Test
    void getSessions은_최근_대화_순으로_조회하고_첫_메시지를_제목으로_사용한다() {
        ChatSession sessionWithMessage = ChatSession.builder().user(user).build();
        setId(sessionWithMessage, 20L);
        ChatSession sessionWithoutMessage = ChatSession.builder().user(user).build();
        setId(sessionWithoutMessage, 21L);

        when(chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(1L))
                .thenReturn(List.of(sessionWithMessage, sessionWithoutMessage));
        ChatMessage firstMessage = ChatMessage.builder()
                .chatSession(sessionWithMessage).role(ChatRole.USER).message("장학금 추천해줘").build();
        when(chatMessageRepository.findFirstByChatSessionIdOrderByCreatedAtAscIdAsc(20L))
                .thenReturn(Optional.of(firstMessage));
        when(chatMessageRepository.findFirstByChatSessionIdOrderByCreatedAtAscIdAsc(21L))
                .thenReturn(Optional.empty());

        List<com.lion.benefito.domain.chatbot.dto.response.ChatSessionListResponse> sessions =
                chatbotService.getSessions(1L);

        assertThat(sessions).hasSize(2);
        assertThat(sessions.get(0).sessionId()).isEqualTo(20L);
        assertThat(sessions.get(0).title()).isEqualTo("장학금 추천해줘");
        assertThat(sessions.get(1).sessionId()).isEqualTo(21L);
        assertThat(sessions.get(1).title()).isEqualTo("새 대화");
    }

    @Test
    void getSessions은_첫_메시지가_길면_제목을_잘라서_말줄임표를_붙인다() {
        ChatSession session = ChatSession.builder().user(user).build();
        setId(session, 20L);
        when(chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(session));
        String longMessage = "가나다라마바사아자차카파타하".repeat(3);
        ChatMessage firstMessage = ChatMessage.builder()
                .chatSession(session).role(ChatRole.USER).message(longMessage).build();
        when(chatMessageRepository.findFirstByChatSessionIdOrderByCreatedAtAscIdAsc(20L))
                .thenReturn(Optional.of(firstMessage));

        List<com.lion.benefito.domain.chatbot.dto.response.ChatSessionListResponse> sessions =
                chatbotService.getSessions(1L);

        assertThat(sessions.get(0).title()).hasSize(33).endsWith("...");
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
    void getMessages는_이전에_저장된_ChatMessagePosting을_통해_그때와_동일한_카드_목록을_복원한다() {
        ChatMessage assistantMessage = ChatMessage.builder()
                .chatSession(chatSession).role(ChatRole.ASSISTANT).message("추천 답변").build();
        setId(assistantMessage, 100L);

        Code categoryCode = mock(Code.class);
        when(categoryCode.getCode()).thenReturn("CONTEST");
        Posting posting = mock(Posting.class);
        when(posting.getId()).thenReturn(202L);
        when(posting.getTitle()).thenReturn("탄소중립 아이디어 공모전");
        when(posting.getCompany()).thenReturn("환경부");
        when(posting.getCategoryCode()).thenReturn(categoryCode);
        when(posting.getEndDate()).thenReturn(java.time.LocalDate.of(2026, 10, 1));
        when(posting.getHomepageUrl()).thenReturn("https://example.com/202");

        ChatMessagePosting chatMessagePosting = ChatMessagePosting.builder()
                .chatMessage(assistantMessage).posting(posting).displayOrder(0).build();

        when(chatSessionRepository.findById(10L)).thenReturn(Optional.of(chatSession));
        when(chatMessageRepository.findByChatSessionIdOrderByCreatedAtAscIdAsc(10L))
                .thenReturn(List.of(assistantMessage));
        when(chatMessagePostingRepository.findByChatSessionIdOrderByChatMessageIdAscDisplayOrderAsc(10L))
                .thenReturn(List.of(chatMessagePosting));

        List<ChatMessageResponse> messages = chatbotService.getMessages(1L, 10L);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).postings()).hasSize(1);
        PostingCardResponse card = messages.get(0).postings().get(0);
        assertThat(card.postingId()).isEqualTo(202L);
        assertThat(card.title()).isEqualTo("탄소중립 아이디어 공모전");
        assertThat(card.organization()).isEqualTo("환경부");
        assertThat(card.categoryCode()).isEqualTo("CONTEST");
        assertThat(card.deadline()).isEqualTo(java.time.LocalDate.of(2026, 10, 1));
        assertThat(card.link()).isEqualTo("https://example.com/202");
    }

    @Test
    void deleteSession은_공고카드_메시지_세션_순으로_세션_단위_배치_삭제한다() {
        when(chatSessionRepository.findById(10L)).thenReturn(Optional.of(chatSession));

        chatbotService.deleteSession(1L, 10L);

        // 메시지/카드 건수와 무관하게 세션 단위 벌크 쿼리 1회씩만 호출되어야 한다
        // (메시지별로 deleteByChatMessageId를 반복 호출하지 않음).
        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(
                chatMessagePostingRepository, chatMessageRepository, chatSessionRepository);
        inOrder.verify(chatMessagePostingRepository).deleteByChatSessionId(10L);
        inOrder.verify(chatMessageRepository).deleteByChatSessionId(10L);
        inOrder.verify(chatSessionRepository).delete(chatSession);

        verify(chatMessagePostingRepository, never()).deleteByChatMessageId(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 다른_사용자_소유_세션을_삭제하려하면_접근이_차단된다() {
        Long otherUserId = 2L;
        when(chatSessionRepository.findById(10L)).thenReturn(Optional.of(chatSession));

        assertThatThrownBy(() -> chatbotService.deleteSession(otherUserId, 10L))
                .isInstanceOf(ChatbotException.class)
                .satisfies(ex -> assertThat(((ChatbotException) ex).getCode())
                        .isEqualTo(ChatbotErrorCode.SESSION_ACCESS_DENIED));

        verify(chatMessagePostingRepository, never()).deleteByChatSessionId(org.mockito.ArgumentMatchers.any());
        verify(chatMessageRepository, never()).deleteByChatSessionId(org.mockito.ArgumentMatchers.any());
        verify(chatSessionRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 존재하지_않는_세션을_삭제하려하면_SESSION_NOT_FOUND_예외() {
        when(chatSessionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatbotService.deleteSession(1L, 999L))
                .isInstanceOf(ChatbotException.class)
                .satisfies(ex -> assertThat(((ChatbotException) ex).getCode())
                        .isEqualTo(ChatbotErrorCode.SESSION_NOT_FOUND));
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
