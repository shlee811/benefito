package com.lion.benefito.infrastructure.external.kstartup;

import java.util.List;

/**
 * K-Startup getAnnouncementInformation01 API의 실제 응답 구조.
 * 문서(서비스설계서)의 XML 예시(<items><item>)와 달리, 실제 JSON 응답은 이 형태의 평평한 구조이며
 * item 배열의 키는 "data"다. 2026-07-14 실제 API 호출(perPage=3, perPage=50)로 직접 확인했다.
 *
 * resultCode/resultMsg는 정상 응답에는 없는 필드다(실제 호출로 확인). data.go.kr 계열 공공 API는
 * 인증키 오류/한도초과 등에도 HTTP 200과 함께 이 형태의 에러 본문을 내려주는 경우가 흔해서,
 * 정상 응답과 구분할 수 있도록 nullable로 함께 받는다.
 */
public record KStartupAnnouncementResponse(
        int currentCount,
        int matchCount,
        int page,
        int perPage,
        int totalCount,
        List<KStartupAnnouncementItem> data,
        String resultCode,
        String resultMsg
) {
    public boolean isErrorResponse() {
        return resultCode != null;
    }
}
