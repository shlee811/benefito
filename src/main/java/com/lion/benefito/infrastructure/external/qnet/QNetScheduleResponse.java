package com.lion.benefito.infrastructure.external.qnet;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@JacksonXmlRootElement(localName = "response")
public record QNetScheduleResponse(
        @JacksonXmlProperty(localName = "header") QNetResponseHeader header,
        @JacksonXmlProperty(localName = "body") Body body
) {
    // QNetQualificationListResponse.Body와 동일한 이유로 일반 클래스(필드 기반 바인딩)를 사용한다.
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Body {
        @JacksonXmlElementWrapper(localName = "items")
        @JacksonXmlProperty(localName = "item")
        private List<QNetScheduleItem> items;
    }
}
