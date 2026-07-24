package com.lion.benefito.domain.posting.exception;

import com.lion.benefito.global.common.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum PostingErrorCode implements BaseErrorCode {

    POSTING_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "존재하지 않는 공고입니다."),
    INVALID_POSTING_SORT(HttpStatus.BAD_REQUEST, "P002", "유효하지 않은 정렬 값입니다."),
    INVALID_POSTING_FILTER(HttpStatus.BAD_REQUEST, "P003", "유효하지 않은 필터 값입니다."),
    INVALID_POSTING_STATUS(HttpStatus.BAD_REQUEST, "P004", "유효하지 않은 공고 상태 값입니다."),
    INVALID_CALENDAR_EVENT_TYPE(HttpStatus.BAD_REQUEST, "P005", "유효하지 않은 캘린더 이벤트 타입 값입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
