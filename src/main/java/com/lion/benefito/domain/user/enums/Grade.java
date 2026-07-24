package com.lion.benefito.domain.user.enums;

public enum Grade {
    GRADE_1,
    GRADE_2,
    GRADE_3,
    GRADE_4,
    LEAVE_ABSENCE, // 휴학생
    GRADUATE; // 졸업생

    public String getDisplayName() {
        return switch (this) {
            case GRADE_1 -> "1학년";
            case GRADE_2 -> "2학년";
            case GRADE_3 -> "3학년";
            case GRADE_4 -> "4학년";
            case LEAVE_ABSENCE -> "휴학생";
            case GRADUATE -> "졸업생";
        };
    }
}
