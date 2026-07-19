package com.lion.benefito.domain.auth.exception;

import com.lion.benefito.global.common.exception.CustomException;
import lombok.Getter;

@Getter
public class AuthException extends CustomException {

    public AuthException(AuthErrorCode errorCode){
        super(errorCode);
    }
}