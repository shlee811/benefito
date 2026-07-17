package com.lion.benefito.domain.chatbot.repository;

import com.lion.benefito.domain.chatbot.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
}
