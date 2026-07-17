package com.lion.benefito.domain.user.exception;

import com.lion.benefito.global.common.exception.CustomException;
import lombok.Getter;

@Getter
public class UserException extends CustomException {

    public UserException(UserErrorCode errorCode) { super(errorCode); }
}
