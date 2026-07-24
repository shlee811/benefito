package com.lion.benefito.domain.todo.service.query;

import com.lion.benefito.domain.todo.dto.response.TodoResDTO;

public interface TodoQueryService {

    TodoResDTO.MyTodoList getMyTodoList(Long userId);
}
