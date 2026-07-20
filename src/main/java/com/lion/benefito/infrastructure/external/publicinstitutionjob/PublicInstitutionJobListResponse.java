package com.lion.benefito.infrastructure.external.publicinstitutionjob;

import java.util.List;

/**
 * "재정경제부_공공기관 채용정보 조회서비스" /list 실제 응답 구조.
 * Swagger 스키마는 result 배열 원소를 {"item": {...}}로 한 번 더 감쌌지만, 실제 응답은
 * item 필드가 배열 원소에 바로 들어있다(2026-07-20 실제 호출로 확인).
 * resultCode는 성공 시 정수 200("성공했습니다"), 페이지가 없을 때도 200 + result=[]로 온다.
 */
public record PublicInstitutionJobListResponse(
        int resultCode,
        String resultMsg,
        int totalCount,
        List<PublicInstitutionJobItem> result
) {
    private static final int SUCCESS_CODE = 200;

    public boolean isSuccess() {
        return resultCode == SUCCESS_CODE;
    }
}
