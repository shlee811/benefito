package com.lion.benefito.infrastructure.ai.rag;

import com.lion.benefito.global.exception.CustomException;

public class RagException extends CustomException {

    public RagException(RagErrorCode errorCode) {
        super(errorCode);
    }
}
