package com.lion.benefito.domain.todo.dto.response;

import lombok.Builder;
import java.time.LocalDate;
import java.util.List;

public class TodoResDTO {

    @Builder
    public record MyTodoList(
            List<TodoItem> todos
    ) {
    }

    @Builder
    public record TodoItem(
            Long todoId,
            String content,
            String categoryName,
            LocalDate endDate,
            Long dDay,
            boolean isCompleted
    ) {
    }
}
