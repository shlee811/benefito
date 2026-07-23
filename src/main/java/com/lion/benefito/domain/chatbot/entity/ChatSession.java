package com.lion.benefito.domain.chatbot.entity;

import com.lion.benefito.domain.user.entity.User;
import com.lion.benefito.global.common.BaseTimeEntity;
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

@Getter
@Entity
@Table(name = "chat_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ChatSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User user;

    // 세션 목록을 "최근 대화 순"으로 정렬하기 위한 용도다. 메시지 저장 시마다 증가시켜 BaseTimeEntity의
    // updatedAt(@LastModifiedDate)이 실제로 갱신되게 만든다(값이 안 바뀌는 필드만 save()해서는 Hibernate가
    // dirty로 보지 않아 updatedAt이 갱신되지 않는다). 기존 행이 있는 테이블이라 DEFAULT를 명시한다.
    @Column(name = "message_count", nullable = false, columnDefinition = "integer default 0")
    private int messageCount;

    public void increaseMessageCount(int count) {
        this.messageCount += count;
    }
}
