package com.lion.benefito.domain.todo.service.query;

import com.lion.benefito.domain.todo.dto.response.TodoResDTO;
import com.lion.benefito.domain.todo.entity.Todo;
import com.lion.benefito.domain.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoQueryServiceImpl implements TodoQueryService {

    private final TodoRepository todoRepository;

    @Override
    public TodoResDTO.MyTodoList getMyTodoList(Long userId) {

        log.info("[ TodoQueryService ] 내 할 일 목록 조회를 시작합니다. userId={}", userId);

        List<Todo> todos = todoRepository.findTodosByUserIdWithCategory(userId);

        LocalDate today = LocalDate.now();

        List<TodoResDTO.TodoItem> todoItems = todos.stream()
                .map(todo -> TodoResDTO.TodoItem.builder()
                        .todoId(todo.getId())
                        .content(todo.getContent())
                        .categoryName(todo.getCategoryCode().getName())
                        .endDate(todo.getEndDate())
                        .dDay(ChronoUnit.DAYS.between(today, todo.getEndDate()))
                        .isCompleted(todo.isCompleted())
                        .build())
                .collect(Collectors.toList());

        log.info("[ TodoQueryService ] 내 할 일 목록 조회가 완료되었습니다. count={}", todoItems.size());

        return TodoResDTO.MyTodoList.builder()
                .todos(todoItems)
                .build();
    }
}
