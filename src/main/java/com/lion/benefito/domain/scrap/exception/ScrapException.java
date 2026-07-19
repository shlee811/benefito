package com.lion.benefito.domain.scrap.exception;

import com.lion.benefito.global.common.exception.CustomException;

public class ScrapException extends CustomException {

    public ScrapException(ScrapErrorCode errorCode) {
        super(errorCode);
    }
}
