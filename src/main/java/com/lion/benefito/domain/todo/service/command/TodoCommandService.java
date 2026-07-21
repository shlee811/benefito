package com.lion.benefito.domain.todo.service.command;

import com.lion.benefito.domain.todo.dto.request.TodoReqDTO;

public interface TodoCommandService {

    void createTodo(Long userId, TodoReqDTO.Create request);

    void updateTodo(Long userId, Long todoId, TodoReqDTO.Update request);

    void toggleTodoStatus(Long userId, Long todoId);

    void deleteTodo(Long userId, Long todoId);
}
