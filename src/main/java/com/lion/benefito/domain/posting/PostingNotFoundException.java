package com.lion.benefito.domain.posting;

import com.lion.benefito.global.exception.CustomException;

public class PostingNotFoundException extends CustomException {

    public PostingNotFoundException(String message) {
        super(message);
    }
}
