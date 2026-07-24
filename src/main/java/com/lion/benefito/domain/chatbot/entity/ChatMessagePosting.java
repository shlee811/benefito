package com.lion.benefito.domain.chatbot.entity;

import com.lion.benefito.domain.posting.entity.Posting;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// sendMessage()가 그 시점에 RAG로 검색해 답변 근거로 쓴 공고 목록을 순서대로 기록해 둔다.
// getMessages()로 과거 대화를 다시 조회할 때도 이 테이블을 통해 그때와 동일한 카드 목록을 복원한다.
@Getter
@Entity
@Table(name = "chat_message_postings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ChatMessagePosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_message_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ChatMessage chatMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posting_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Posting posting;

    // RAG 검색 결과(유사도 순) 순서를 그대로 보존해, 히스토리 재조회 시에도 카드 순서가 바뀌지 않게 한다.
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
