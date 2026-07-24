package com.lion.benefito.domain.chatbot.repository;

import com.lion.benefito.domain.chatbot.entity.ChatSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    // updatedAt은 메시지가 저장될 때마다 ChatSession.increaseMessageCount()로 함께 갱신되므로
    // 곧 "최근 대화 순"이 된다.
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
