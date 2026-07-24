package com.lion.benefito.domain.chatbot.exception;

import com.lion.benefito.global.common.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatbotErrorCode implements BaseErrorCode {

    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "CHATBOT404_1", "존재하지 않는 대화 세션입니다."),
    SESSION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CHATBOT403_1", "해당 대화 세션에 접근할 권한이 없습니다."),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CHATBOT404_2", "존재하지 않는 메시지입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
