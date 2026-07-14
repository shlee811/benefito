package com.lion.benefito.domain.code.entity;

import com.lion.benefito.domain.code.CodeException;

public enum CommonCodeType {
    CATEGORY,
    REGION,
    GRADE;

    public static CommonCodeType from(String type) {
        if (type == null) {
            throw new CodeException("codeType은 필수입니다.");
        }
        try {
            return CommonCodeType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CodeException("유효하지 않은 codeType 입니다: " + type);
        }
    }
}
