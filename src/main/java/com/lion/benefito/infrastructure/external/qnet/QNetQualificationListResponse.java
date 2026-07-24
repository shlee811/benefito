package com.lion.benefito.infrastructure.external.qnet;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// InquiryListNationalQualifcationSVC/getList 실제 응답 구조(2026-07-21 실제 호출로 확인).
@JacksonXmlRootElement(localName = "response")
public record QNetQualificationListResponse(
        @JacksonXmlProperty(localName = "header") QNetResponseHeader header,
        @JacksonXmlProperty(localName = "body") Body body
) {
    // record + @JacksonXmlElementWrapper 조합은 Jackson이 creator 프로퍼티 이름을 잘못 매칭해서
    // (items 래퍼와 item 리스트를 같은 프로퍼티로 착각) 역직렬화가 실패한다(실제로 재현됨).
    // 필드 기반 바인딩을 쓰는 일반 클래스로 우회한다.
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Body {
        @JacksonXmlElementWrapper(localName = "items")
        @JacksonXmlProperty(localName = "item")
        private List<QNetQualificationItem> items;
    }
}
