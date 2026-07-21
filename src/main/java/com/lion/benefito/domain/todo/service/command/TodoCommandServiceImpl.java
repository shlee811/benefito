package com.lion.benefito.domain.todo.service.command;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.code.repository.CodeRepository;
import com.lion.benefito.domain.todo.dto.request.TodoReqDTO;
import com.lion.benefito.domain.todo.entity.Todo;
import com.lion.benefito.domain.todo.exception.TodoErrorCode;
import com.lion.benefito.domain.todo.repository.TodoRepository;
import com.lion.benefito.domain.user.entity.User;
import com.lion.benefito.domain.user.exception.UserErrorCode;
import com.lion.benefito.domain.user.repository.UserRepository;
import com.lion.benefito.global.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TodoCommandServiceImpl implements TodoCommandService {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    private final CodeRepository codeRepository;

    @Override
    public void createTodo(Long userId, TodoReqDTO.Create request) {

        log.info("[ TodoCommandService ] 할 일 생성을 시작합니다. userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        Code categoryCode = codeRepository.findByCode(request.categoryCode())
                .orElseThrow(() -> new CustomException(TodoErrorCode.INVALID_CATEGORY));

        Todo todo = Todo.builder()
                .user(user)
                .categoryCode(categoryCode)
                .content(request.content())
                .endDate(request.endDate())
                .isCompleted(false)
                .build();

        todoRepository.save(todo);

        log.info("[ TodoCommandService ] 할 일 생성이 완료되었습니다. todoId={}", todo.getId());
    }

    @Override
    public void updateTodo(Long userId, Long todoId, TodoReqDTO.Update request) {

        log.info("[ TodoCommandService ] 할 일 수정을 시작합니다. userId={}, todoId={}", userId, todoId);

        Todo todo = getTodoWithOwnershipCheck(userId, todoId);

        Code categoryCode = codeRepository.findByCode(request.categoryCode())
                .orElseThrow(() -> new CustomException(TodoErrorCode.INVALID_CATEGORY));

        todo.updateTodo(request.content(), request.endDate(), categoryCode);

        log.info("[ TodoCommandService ] 할 일 수정이 완료되었습니다. todoId={}", todoId);
    }

    @Override
    public void toggleTodoStatus(Long userId, Long todoId) {

        log.info("[ TodoCommandService ] 할 일 상태 변경을 시작합니다. userId={}, todoId={}", userId, todoId);

        Todo todo = getTodoWithOwnershipCheck(userId, todoId);
        todo.toggleComplete();

        log.info("[ TodoCommandService ] 할 일 상태 변경이 완료되었습니다. todoId={}, isCompleted={}", todoId, todo.isCompleted());
    }

    @Override
    public void deleteTodo(Long userId, Long todoId) {

        log.info("[ TodoCommandService ] 할 일 삭제를 시작합니다. userId={}, todoId={}", userId, todoId);

        Todo todo = getTodoWithOwnershipCheck(userId, todoId);
        todoRepository.delete(todo);

        log.info("[ TodoCommandService ] 할 일 삭제가 완료되었습니다. todoId={}", todoId);
    }

    private Todo getTodoWithOwnershipCheck(Long userId, Long todoId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new CustomException(TodoErrorCode.TODO_NOT_FOUND));

        if (!todo.getUser().getId().equals(userId)) {
            throw new CustomException(TodoErrorCode.UNAUTHORIZED_TODO_ACCESS);
        }

        return todo;
    }
}
