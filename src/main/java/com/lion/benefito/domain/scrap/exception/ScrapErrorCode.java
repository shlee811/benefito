package com.lion.benefito.domain.scrap.exception;

import com.lion.benefito.global.common.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ScrapErrorCode implements BaseErrorCode {

    SCRAP_NOT_FOUND(HttpStatus.NOT_FOUND, "SCRAP404_1", "스크랩하지 않은 공고입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
