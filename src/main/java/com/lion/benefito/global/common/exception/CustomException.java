package com.lion.benefito.global.common.exception;

import com.lion.benefito.global.common.code.BaseErrorCode;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final BaseErrorCode Code;

    public CustomException(BaseErrorCode code) {
        this.Code = code;
    }
}
