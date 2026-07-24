package com.lion.benefito.domain.scrap.exception;

import com.lion.benefito.global.common.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ScrapErrorCode implements BaseErrorCode {

    SCRAP_NOT_FOUND(HttpStatus.NOT_FOUND, "SCRAP404_1", "스크랩하지 않은 공고입니다."),
    SCRAP_ALREADY_EXISTS(HttpStatus.CONFLICT, "SCRAP409_1", "이미 스크랩한 공고입니다."),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "SCRAP400_1", "유효하지 않은 카테고리명입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
