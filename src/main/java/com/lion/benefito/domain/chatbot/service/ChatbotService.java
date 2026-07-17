package com.lion.benefito.domain.chatbot.service;

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
import com.lion.benefito.domain.user.entity.User;
import com.lion.benefito.domain.user.exception.UserErrorCode;
import com.lion.benefito.domain.user.exception.UserException;
import com.lion.benefito.domain.user.repository.UserRepository;
import com.lion.benefito.infrastructure.ai.chat.AiChatClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final AiChatClient aiChatClient;

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

    @Transactional
    public ChatMessageResponse sendMessage(Long userId, Long sessionId, ChatMessageRequest request) {
        ChatSession chatSession = getOwnedSession(userId, sessionId);

        ChatMessage userMessage = ChatMessage.builder()
                .chatSession(chatSession)
                .role(ChatRole.USER)
                .message(request.message())
                .build();
        chatMessageRepository.save(userMessage);

        String reply = aiChatClient.generateReply(request.message(), request.postingId());

        ChatMessage assistantMessage = ChatMessage.builder()
                .chatSession(chatSession)
                .role(ChatRole.ASSISTANT)
                .message(reply)
                .build();
        chatMessageRepository.save(assistantMessage);

        return ChatMessageResponse.from(assistantMessage);
    }

    public List<ChatMessageResponse> getMessages(Long userId, Long sessionId) {
        getOwnedSession(userId, sessionId);

        return chatMessageRepository.findByChatSessionIdOrderByCreatedAtAscIdAsc(sessionId)
                .stream()
                .map(ChatMessageResponse::from)
                .toList();
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
