package com.lion.benefito.infrastructure.ai.chat;

import com.lion.benefito.global.exception.CustomException;

public class ChatException extends CustomException {

    public ChatException(ChatErrorCode errorCode) {
        super(errorCode);
    }
}
