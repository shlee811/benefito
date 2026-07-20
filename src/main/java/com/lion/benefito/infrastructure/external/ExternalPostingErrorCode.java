package com.lion.benefito.infrastructure.external;

import com.lion.benefito.global.common.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ExternalPostingErrorCode implements BaseErrorCode {
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "EXT001", "외부 API 인증에 실패했습니다."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "EXT002", "외부 API 요청 한도를 초과했습니다."),
    API_CALL_FAILED(HttpStatus.BAD_GATEWAY, "EXT003", "외부 API 호출에 실패했습니다."),
    INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "EXT004", "외부 API 응답을 해석할 수 없습니다."),
    CATEGORY_CODE_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "EXT005", "공고에 사용할 카테고리 공통 코드가 설정되어 있지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
