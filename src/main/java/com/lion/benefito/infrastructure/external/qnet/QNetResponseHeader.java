package com.lion.benefito.infrastructure.external.qnet;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Q-Net 두 오퍼레이션(getList, getJMList) 모두 동일한 header 구조를 쓴다.
 * 실제 호출로 확인: 성공 시 resultCode="00"(문자열), 실패 시에도 항상 resultCode/resultMsg가 존재한다.
 */
public record QNetResponseHeader(
        @JacksonXmlProperty(localName = "resultCode") String resultCode,
        @JacksonXmlProperty(localName = "resultMsg") String resultMsg
) {
    private static final String SUCCESS_CODE = "00";

    public boolean isSuccess() {
        return SUCCESS_CODE.equals(resultCode);
    }
}
