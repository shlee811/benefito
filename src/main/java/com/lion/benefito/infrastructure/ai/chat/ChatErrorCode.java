package com.lion.benefito.infrastructure.ai.chat;

import com.lion.benefito.global.common.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatErrorCode implements BaseErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "CHAT400_1", "채팅 메시지가 비어있습니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "CHAT401_1", "OpenAI API 인증에 실패했습니다."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "CHAT429_1", "OpenAI API 요청 한도를 초과했습니다."),
    API_ERROR(HttpStatus.BAD_GATEWAY, "CHAT502_1", "OpenAI Chat API 호출에 실패했습니다."),
    EMPTY_RESPONSE(HttpStatus.BAD_GATEWAY, "CHAT502_2", "OpenAI 응답 본문이 비어있습니다."),
    EMPTY_CHOICES(HttpStatus.BAD_GATEWAY, "CHAT502_3", "OpenAI 응답에 choices 항목이 없습니다."),
    EMPTY_CONTENT(HttpStatus.BAD_GATEWAY, "CHAT502_4", "OpenAI 응답 메시지 내용이 비어있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
