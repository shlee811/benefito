package com.lion.benefito.domain.code.exception;

import com.lion.benefito.global.common.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum CodeErrorCode implements BaseErrorCode {

    CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "CO001", "해당 공통 코드를 찾을 수 없습니다."),
    INVALID_CODE_TYPE(HttpStatus.BAD_REQUEST, "CO002", "유효하지 않은 공통 코드 타입입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
