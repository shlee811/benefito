package com.lion.benefito.domain.user.enums;

public enum Income {
    INCOME_1,
    INCOME_2,
    INCOME_3,
    INCOME_4,
    INCOME_5,
    INCOME_6,
    INCOME_7,
    INCOME_8,
    INCOME_9,
    INCOME_10;

    public String getDisplayName() {
        String tier = name().substring("INCOME_".length());
        return tier + "분위";
    }
}
