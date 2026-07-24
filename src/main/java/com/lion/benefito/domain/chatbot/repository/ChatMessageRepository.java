package com.lion.benefito.domain.chatbot.repository;

import com.lion.benefito.domain.chatbot.entity.ChatMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatSessionIdOrderByCreatedAtAscIdAsc(Long chatSessionId);

    // 세션 목록의 title을 이 세션의 첫 메시지(보통 사용자의 첫 질문)로 만들기 위해 사용한다.
    Optional<ChatMessage> findFirstByChatSessionIdOrderByCreatedAtAscIdAsc(Long chatSessionId);
}
