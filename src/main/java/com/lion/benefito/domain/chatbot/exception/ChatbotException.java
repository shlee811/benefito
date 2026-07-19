package com.lion.benefito.domain.chatbot.exception;

import com.lion.benefito.global.common.exception.CustomException;

public class ChatbotException extends CustomException {

    public ChatbotException(ChatbotErrorCode errorCode) {
        super(errorCode);
    }
}
