package com.lion.benefito.domain.posting.entity;

import com.lion.benefito.domain.posting.exception.PostingErrorCode;
import com.lion.benefito.domain.posting.exception.PostingException;

public enum CalendarEventType {
    START,
    DEADLINE;

    // 값이 없으면 null을 반환해 START/DEADLINE 모두 포함하도록 한다(필터 없음이 기본값).
    public static CalendarEventType from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return CalendarEventType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PostingException(PostingErrorCode.INVALID_CALENDAR_EVENT_TYPE);
        }
    }
}
