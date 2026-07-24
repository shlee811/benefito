package com.lion.benefito.domain.todo.entity;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.user.entity.User;
import com.lion.benefito.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Todo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_code_id", nullable = false)
    private Code categoryCode;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean isCompleted;

    // 할 일 수정 비즈니스 로직
    public void updateTodo(String content, LocalDate endDate, Code categoryCode) {
        this.content = content;
        this.endDate = endDate;
        this.categoryCode = categoryCode;
    }

    // 할 일 완료 여부 토글 비즈니스 로직
    public void toggleComplete() {
        this.isCompleted = !this.isCompleted;
    }
}
