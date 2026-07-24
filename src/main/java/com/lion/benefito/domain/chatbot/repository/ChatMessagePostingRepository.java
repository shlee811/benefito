package com.lion.benefito.domain.chatbot.repository;

import com.lion.benefito.domain.chatbot.entity.ChatMessagePosting;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessagePostingRepository extends JpaRepository<ChatMessagePosting, Long> {

    // 세션 전체 메시지의 카드를 한 번에 조회해 N+1을 피한다. Posting과 카드 렌더링에 쓰이는
    // categoryCode/regionCode까지 JOIN FETCH해서 지연 로딩 예외 없이 바로 PostingCardResponse로 변환한다.
    @Query("""
            SELECT cmp FROM ChatMessagePosting cmp
            JOIN FETCH cmp.posting p
            LEFT JOIN FETCH p.categoryCode
            LEFT JOIN FETCH p.regionCode
            WHERE cmp.chatMessage.chatSession.id = :sessionId
            ORDER BY cmp.chatMessage.id ASC, cmp.displayOrder ASC
            """)
    List<ChatMessagePosting> findByChatSessionIdOrderByChatMessageIdAscDisplayOrderAsc(
            @Param("sessionId") Long sessionId);

    // 메시지 삭제 시 그 메시지에 달린 공고 카드를 먼저 지운다(FK 제약은 없지만 ChatMessage 삭제보다
    // 먼저 실행되어야 한다).
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ChatMessagePosting cmp WHERE cmp.chatMessage.id = :messageId")
    void deleteByChatMessageId(@Param("messageId") Long messageId);

    // 세션 전체 삭제 시 그 세션에 속한 모든 메시지의 공고 카드를 한 번에 지운다
    // (deleteByChatMessageId를 메시지 수만큼 반복 호출하지 않기 위함).
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ChatMessagePosting cmp WHERE cmp.chatMessage.chatSession.id = :sessionId")
    void deleteByChatSessionId(@Param("sessionId") Long sessionId);
}
