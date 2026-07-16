package com.lion.benefito.domain.code.exception;

import com.lion.benefito.global.exception.CustomException;
import lombok.Getter;

@Getter
public class CodeException extends CustomException {

    public CodeException(CodeErrorCode errorCode) {
        super(errorCode);
    }
}
