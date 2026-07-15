package com.lion.benefito.domain.posting;

import com.lion.benefito.global.exception.CustomException;

public class PostingException extends CustomException {

    public PostingException(String message) {
        super(message);
    }
}
