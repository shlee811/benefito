package com.lion.benefito.infrastructure.ai.rag;

import com.lion.benefito.global.common.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RagErrorCode implements BaseErrorCode {

    INVALID_TOP_K(HttpStatus.BAD_REQUEST, "RAG400_1", "topK는 1 이상이어야 합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
