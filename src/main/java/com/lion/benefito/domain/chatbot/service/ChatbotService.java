package com.lion.benefito.domain.chatbot.service;

import com.lion.benefito.domain.chatbot.dto.request.ChatMessageRequest;
import com.lion.benefito.domain.chatbot.dto.response.ChatMessageResponse;
import com.lion.benefito.domain.chatbot.dto.response.ChatSessionCreateResponse;
import com.lion.benefito.domain.chatbot.dto.response.ChatSessionListResponse;
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
import com.lion.benefito.domain.posting.dto.PostingSimilarityResponse;
import com.lion.benefito.domain.posting.repository.PostingRepository;
import com.lion.benefito.domain.user.entity.User;
import com.lion.benefito.domain.user.exception.UserErrorCode;
import com.lion.benefito.domain.user.exception.UserException;
import com.lion.benefito.domain.user.repository.UserInterestRepository;
import com.lion.benefito.domain.user.repository.UserRepository;
import com.lion.benefito.infrastructure.ai.chat.AiChatClient;
import com.lion.benefito.infrastructure.ai.chat.ChatPromptFactory;
import com.lion.benefito.infrastructure.ai.chat.OpenAiChatMessage;
import com.lion.benefito.infrastructure.ai.chat.UserProfileContext;
import com.lion.benefito.infrastructure.ai.rag.PostingRetriever;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    // RAG 검색 시 조회할 유사 공고 개수
    private static final int SIMILAR_POSTING_TOP_K = 5;
    private static final int SESSION_TITLE_MAX_LENGTH = 30;
    private static final String DEFAULT_SESSION_TITLE = "새 대화";

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final UserInterestRepository userInterestRepository;
    private final AiChatClient aiChatClient;
    private final PostingRetriever postingRetriever;
    private final ChatPromptFactory chatPromptFactory;
    private final PostingRepository postingRepository;
    private final ChatMessagePostingRepository chatMessagePostingRepository;

    @Transactional
    public ChatSessionCreateResponse createSession(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        ChatSession chatSession = ChatSession.builder()
                .user(user)
                .build();
        chatSessionRepository.save(chatSession);

        return new ChatSessionCreateResponse(chatSession.getId());
    }

    // 트랜잭션 정책: 메서드 전체를 하나의 @Transactional로 묶지 않는다. RAG 검색과 AI 응답 생성은
    // 둘 다 외부 API 호출(OpenAI)이라 시간이 걸릴 수 있는데, 하나의 트랜잭션 안에 있으면 그 시간 동안
    // DB 커넥션을 계속 붙잡게 되고, 무엇보다 AI 호출이 실패했을 때 이미 저장한 사용자 메시지까지
    // 같이 롤백되어 사라지는 문제가 있었다(정책: AI 호출이 실패해도 사용자가 보낸 메시지는 남아야 한다).
    // 그래서 사용자 메시지 저장은 그 자체로 즉시 커밋되고, AI 호출이 실패하면 예외가 그대로 호출자에게
    // 전파되며 어시스턴트 메시지는 저장되지 않는다 - 다음에 getMessages()로 조회하면 사용자 메시지는
    // 남아있고 그에 대한 답변만 없는 상태가 된다.
    public ChatMessageResponse sendMessage(Long userId, Long sessionId, ChatMessageRequest request) {
        ChatSession chatSession = getOwnedSession(userId, sessionId);

        ChatMessage userMessage = ChatMessage.builder()
                .chatSession(chatSession)
                .role(ChatRole.USER)
                .message(request.message())
                .build();
        chatMessageRepository.save(userMessage);
        touchSession(chatSession);

        // PostingRetriever 내부에서 질문 임베딩 + pgvector 검색을 모두 수행하므로
        // 여기서 EmbeddingClient를 별도로 호출하지 않는다.
        List<PostingSimilarityResponse> searchResults =
                postingRetriever.findSimilarPostings(request.message(), SIMILAR_POSTING_TOP_K);
        UserProfileContext userProfile = buildUserProfileContext(userId);
        List<OpenAiChatMessage> chatMessages =
                chatPromptFactory.createChatMessages(request.message(), searchResults, userProfile);
        String reply = aiChatClient.generateReply(chatMessages);

        ChatMessage assistantMessage = ChatMessage.builder()
                .chatSession(chatSession)
                .role(ChatRole.ASSISTANT)
                .message(reply)
                .build();
        chatMessageRepository.save(assistantMessage);
        touchSession(chatSession);

        // 프롬프트 조립에 쓴 searchResults 중, AI가 실제로 답변 본문에서 이름을 언급한 공고만 카드로
        // 남긴다(별도 조회 없이 같은 RAG 검색 결과 안에서만 고른다) - 검색은 됐지만 답변에서 언급되지
        // 않은 후보까지 카드로 노출되는 것을 막기 위함이다.
        List<PostingSimilarityResponse> mentionedResults = searchResults.stream()
                .filter(posting -> isMentionedInReply(posting, reply))
                .toList();
        List<PostingCardResponse> postingCards = mentionedResults.stream()
                .map(PostingCardResponse::from)
                .toList();
        savePostingCards(assistantMessage, mentionedResults);

        return ChatMessageResponse.from(assistantMessage, postingCards);
    }

    private boolean isMentionedInReply(PostingSimilarityResponse posting, String reply) {
        return posting.title() != null && !posting.title().isBlank() && reply.contains(posting.title());
    }

    public List<ChatMessageResponse> getMessages(Long userId, Long sessionId) {
        getOwnedSession(userId, sessionId);

        List<ChatMessage> messages = chatMessageRepository.findByChatSessionIdOrderByCreatedAtAscIdAsc(sessionId);
        Map<Long, List<PostingCardResponse>> postingsByMessageId = chatMessagePostingRepository
                .findByChatSessionIdOrderByChatMessageIdAscDisplayOrderAsc(sessionId)
                .stream()
                .collect(Collectors.groupingBy(
                        cmp -> cmp.getChatMessage().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(cmp -> PostingCardResponse.from(cmp.getPosting()), Collectors.toList())));

        return messages.stream()
                .map(message -> ChatMessageResponse.from(
                        message, postingsByMessageId.getOrDefault(message.getId(), List.of())))
                .toList();
    }

    // RAG 검색 결과(유사도 순)를 순서 그대로 저장해, getMessages()로 다시 조회할 때도 같은 카드 목록을
    // 같은 순서로 복원할 수 있게 한다.
    private void savePostingCards(ChatMessage assistantMessage, List<PostingSimilarityResponse> searchResults) {
        List<ChatMessagePosting> chatMessagePostings = new ArrayList<>();
        for (int i = 0; i < searchResults.size(); i++) {
            chatMessagePostings.add(ChatMessagePosting.builder()
                    .chatMessage(assistantMessage)
                    .posting(postingRepository.getReferenceById(searchResults.get(i).postingId()))
                    .displayOrder(i)
                    .build());
        }
        chatMessagePostingRepository.saveAll(chatMessagePostings);
    }

    // 로그인 사용자의 세션 목록을 최근 대화(마지막 메시지 기준) 순으로 반환한다.
    public List<ChatSessionListResponse> getSessions(Long userId) {
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(chatSession -> ChatSessionListResponse.of(chatSession, buildSessionTitle(chatSession.getId())))
                .toList();
    }

    // 메시지가 저장될 때마다 세션의 messageCount를 늘려 저장한다. 값이 바뀌어야 Hibernate가 실제로
    // UPDATE를 실행하고, 그래야 BaseTimeEntity의 updatedAt(@LastModifiedDate)도 함께 갱신되어
    // "최근 대화 순" 정렬의 기준이 된다.
    private void touchSession(ChatSession chatSession) {
        chatSession.increaseMessageCount(1);
        chatSessionRepository.save(chatSession);
    }

    private String buildSessionTitle(Long sessionId) {
        return chatMessageRepository.findFirstByChatSessionIdOrderByCreatedAtAscIdAsc(sessionId)
                .map(ChatMessage::getMessage)
                .map(this::truncateTitle)
                .orElse(DEFAULT_SESSION_TITLE);
    }

    private String truncateTitle(String message) {
        String trimmed = message.strip();
        return trimmed.length() <= SESSION_TITLE_MAX_LENGTH
                ? trimmed
                : trimmed.substring(0, SESSION_TITLE_MAX_LENGTH) + "...";
    }

    private UserProfileContext buildUserProfileContext(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        List<String> categoryNames = userInterestRepository.findByUserId(userId).stream()
                .map(interest -> interest.getCategoryCode().getName())
                .toList();

        String gradeName = user.getGrade() != null ? user.getGrade().getDisplayName() : null;
        String incomeName = user.getIncome() != null ? user.getIncome().getDisplayName() : null;
        String regionName = user.getRegionCode() != null ? user.getRegionCode().getName() : null;

        return new UserProfileContext(gradeName, incomeName, regionName, categoryNames);
    }

    // 세션 안의 메시지 1건만 지운다. FK 제약이 없어 참조 무결성이 DB 레벨에서 보장되지 않으므로,
    // 자식(ChatMessagePosting) -> 부모(ChatMessage) 순서로 직접 삭제한다.
    @Transactional
    public void deleteMessage(Long userId, Long sessionId, Long messageId) {
        getOwnedSession(userId, sessionId);

        ChatMessage chatMessage = chatMessageRepository.findById(messageId)
                .filter(message -> message.getChatSession().getId().equals(sessionId))
                .orElseThrow(() -> new ChatbotException(ChatbotErrorCode.MESSAGE_NOT_FOUND));

        chatMessagePostingRepository.deleteByChatMessageId(messageId);
        chatMessageRepository.delete(chatMessage);
    }

    private ChatSession getOwnedSession(Long userId, Long sessionId) {
        ChatSession chatSession = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ChatbotException(ChatbotErrorCode.SESSION_NOT_FOUND));

        if (!chatSession.getUser().getId().equals(userId)) {
            throw new ChatbotException(ChatbotErrorCode.SESSION_ACCESS_DENIED);
        }
        return chatSession;
    }
}
