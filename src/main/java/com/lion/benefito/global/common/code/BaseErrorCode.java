package com.lion.benefito.global.common.code;

import com.lion.benefito.global.common.CustomResponse;
import org.springframework.http.HttpStatus;

public interface BaseErrorCode {
    HttpStatus getHttpStatus();
    String getCode();
    String getMessage();

    default CustomResponse<Void> getErrorResponse() {
        return CustomResponse.onFailure(getCode(), getMessage());
    }
}
