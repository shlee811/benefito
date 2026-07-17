package com.lion.benefito.infrastructure.ai.embedding;

import com.lion.benefito.global.common.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EmbeddingErrorCode implements BaseErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "EMBEDDING400_1", "임베딩할 텍스트가 비어있습니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "EMBEDDING401_1", "OpenAI API 인증에 실패했습니다."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "EMBEDDING429_1", "OpenAI API 요청 한도를 초과했습니다."),
    API_ERROR(HttpStatus.BAD_GATEWAY, "EMBEDDING502_1", "OpenAI Embedding API 호출에 실패했습니다."),
    EMPTY_RESPONSE(HttpStatus.BAD_GATEWAY, "EMBEDDING502_2", "OpenAI 응답 본문이 비어있습니다."),
    EMPTY_DATA(HttpStatus.BAD_GATEWAY, "EMBEDDING502_3", "OpenAI 응답에 data 항목이 없습니다."),
    EMPTY_EMBEDDING(HttpStatus.BAD_GATEWAY, "EMBEDDING502_4", "OpenAI 응답에 embedding 벡터가 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
