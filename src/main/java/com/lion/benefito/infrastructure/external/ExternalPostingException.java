package com.lion.benefito.infrastructure.external;

import com.lion.benefito.global.common.exception.CustomException;
import lombok.Getter;

@Getter
public class ExternalPostingException extends CustomException {

    public ExternalPostingException(ExternalPostingErrorCode errorCode) {
        super(errorCode);
    }
}
