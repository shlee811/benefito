package com.lion.benefito.domain.posting.exception;

import com.lion.benefito.global.common.exception.CustomException;
import lombok.Getter;

@Getter
public class PostingException extends CustomException {

    public PostingException(PostingErrorCode errorCode) {
        super(errorCode);
    }
}
