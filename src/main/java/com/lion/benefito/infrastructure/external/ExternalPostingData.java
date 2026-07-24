package com.lion.benefito.infrastructure.external;

import java.time.LocalDate;

/**
 * 외부 소스(K-Startup, 고용24, Q-Net, 장학재단 등)에서 수집한 공고를 표현하는 공통 모델.
 * 각 ExternalPostingSource 구현체는 외부 응답 DTO를 이 모델로 변환해서 돌려주고,
 * PostingMapper는 이 모델만 알면 되므로 소스가 늘어나도 이후 파이프라인은 그대로 재사용된다.
 *
 * category/region은 원본 문자열을 그대로 보존한다(Code 매핑은 PostingMapper의 책임).
 *
 * salary/employmentType/enterpriseType/career/education은 채용(JOB) 성격의 소스만 채우는
 * 선택 필드다(그 외 카테고리는 개념 자체가 안 맞아 null). 기존 7개 소스(KStartup/공공기관 채용/
 * Q-Net/장학금/SEED/SEED_QNET_SCHEDULE/SEED_INTERN)는 모두 이 필드들이 생기기 전부터 쓰던
 * 짧은 생성자를 그대로 호출하므로 코드 변경이 전혀 필요 없다 - 아래 보조 생성자가 5개 필드를
 * null로 채워 위임한다.
 */
public record ExternalPostingData(
        String sourceType,
        String externalId,
        String title,
        String content,
        String organization,
        String originalUrl,
        String category,
        String region,
        String qualification,
        LocalDate startDate,
        LocalDate deadline,
        String salary,
        String employmentType,
        String enterpriseType,
        String career,
        String education
) {

    // 기존 소스 전용 하위 호환 생성자. salary/employmentType/enterpriseType/career/education
    // 개념이 없는 소스는 이 생성자를 그대로 쓰면 된다(신규 필드는 전부 null로 채워짐).
    public ExternalPostingData(
            String sourceType, String externalId, String title, String content, String organization,
            String originalUrl, String category, String region, String qualification,
            LocalDate startDate, LocalDate deadline) {
        this(sourceType, externalId, title, content, organization, originalUrl, category, region,
                qualification, startDate, deadline, null, null, null, null, null);
    }
}
