package com.lion.benefito.domain.todo.exception;

import com.lion.benefito.global.common.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TodoErrorCode implements BaseErrorCode {

    TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "TODO404_1", "존재하지 않는 할 일입니다."),
    UNAUTHORIZED_TODO_ACCESS(HttpStatus.FORBIDDEN, "TODO403_1", "자신의 할 일만 접근할 수 있습니다."),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "TODO400_1", "유효하지 않은 카테고리명입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
