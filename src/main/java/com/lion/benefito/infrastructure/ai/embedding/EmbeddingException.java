package com.lion.benefito.infrastructure.ai.embedding;

import com.lion.benefito.global.common.exception.CustomException;

public class EmbeddingException extends CustomException {

    public EmbeddingException(EmbeddingErrorCode errorCode) {
        super(errorCode);
    }
}
