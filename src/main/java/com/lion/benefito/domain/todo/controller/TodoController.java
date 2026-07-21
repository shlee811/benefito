package com.lion.benefito.domain.todo.controller;

import com.lion.benefito.domain.todo.dto.request.TodoReqDTO;
import com.lion.benefito.domain.todo.dto.response.TodoResDTO;
import com.lion.benefito.domain.todo.service.command.TodoCommandService;
import com.lion.benefito.domain.todo.service.query.TodoQueryService;
import com.lion.benefito.global.common.CustomResponse;
import com.lion.benefito.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Todo API", description = "투두리스트 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/todos")
public class TodoController {

    private final TodoCommandService todoCommandService;
    private final TodoQueryService todoQueryService;

    @Operation(summary = "할 일 생성", description = "로그인 사용자가 새로운 할 일을 생성한다.")
    @PostMapping
    public CustomResponse<String> createTodo(
            @Valid @RequestBody TodoReqDTO.Create request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        todoCommandService.createTodo(userId, request);

        return CustomResponse.onSuccess("할 일 생성 성공");
    }

    @Operation(summary = "내 할 일 목록 조회", description = "로그인 사용자의 할 일 목록을 정렬된 상태로 조회한다.")
    @GetMapping("/my")
    public CustomResponse<TodoResDTO.MyTodoList> getMyTodoList(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        TodoResDTO.MyTodoList response = todoQueryService.getMyTodoList(userId);

        return CustomResponse.onSuccess(response);
    }

    @Operation(summary = "할 일 수정", description = "특정 할 일의 내용, 마감일, 카테고리를 수정한다.")
    @PatchMapping("/{todoId}")
    public CustomResponse<String> updateTodo(
            @PathVariable Long todoId,
            @Valid @RequestBody TodoReqDTO.Update request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        todoCommandService.updateTodo(userId, todoId, request);

        return CustomResponse.onSuccess("할 일 수정 성공");
    }

    @Operation(summary = "할 일 완료 여부 토글", description = "특정 할 일의 완료 상태를 반전(완료<->미완료)시킨다.")
    @PatchMapping("/{todoId}/status")
    public CustomResponse<String> toggleTodoStatus(
            @PathVariable Long todoId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        todoCommandService.toggleTodoStatus(userId, todoId);

        return CustomResponse.onSuccess("할 일 상태 변경 성공");
    }

    @Operation(summary = "할 일 삭제", description = "특정 할 일을 삭제한다.")
    @DeleteMapping("/{todoId}")
    public CustomResponse<String> deleteTodo(
            @PathVariable Long todoId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        todoCommandService.deleteTodo(userId, todoId);

        return CustomResponse.onSuccess("할 일 삭제 성공");
    }
}
