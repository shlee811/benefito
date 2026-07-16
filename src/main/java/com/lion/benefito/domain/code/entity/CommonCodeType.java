package com.lion.benefito.domain.code.entity;

import com.lion.benefito.domain.code.exception.CodeErrorCode;
import com.lion.benefito.domain.code.exception.CodeException;

public enum CommonCodeType {
    CATEGORY,
    REGION,
    GRADE;

    public static CommonCodeType from(String type) {
        if (type == null) {
            throw new CodeException(CodeErrorCode.INVALID_CODE_TYPE);
        }
        try {
            return CommonCodeType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CodeException(CodeErrorCode.INVALID_CODE_TYPE);
        }
    }
}
