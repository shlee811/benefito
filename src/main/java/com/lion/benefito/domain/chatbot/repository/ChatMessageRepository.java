package com.lion.benefito.domain.chatbot.repository;

import com.lion.benefito.domain.chatbot.entity.ChatMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatSessionIdOrderByCreatedAtAscIdAsc(Long chatSessionId);

    // 세션 목록의 title을 이 세션의 첫 메시지(보통 사용자의 첫 질문)로 만들기 위해 사용한다.
    Optional<ChatMessage> findFirstByChatSessionIdOrderByCreatedAtAscIdAsc(Long chatSessionId);

    // 세션 삭제 시 그 세션의 메시지를 한 번에 지운다(메시지 수만큼 delete를 반복하지 않기 위한 벌크 쿼리).
    // ChatbotService.deleteSession()에서 chatMessagePostingRepository.deleteByChatSessionId()로
    // 공고 카드를 먼저 지운 뒤 호출해야 한다(FK 제약이 없어 순서를 코드로 보장해야 함).
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ChatMessage m WHERE m.chatSession.id = :sessionId")
    void deleteByChatSessionId(@Param("sessionId") Long sessionId);
}
