package com.lion.benefito.domain.chatbot.repository;

import com.lion.benefito.domain.chatbot.entity.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatSessionIdOrderByCreatedAtAscIdAsc(Long chatSessionId);
}
