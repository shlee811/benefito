package com.lion.benefito.domain.chatbot.exception;

import com.lion.benefito.global.exception.CustomException;

public class ChatbotException extends CustomException {

    public ChatbotException(ChatbotErrorCode errorCode) {
        super(errorCode);
    }
}
