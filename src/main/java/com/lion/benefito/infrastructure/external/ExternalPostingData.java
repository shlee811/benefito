package com.lion.benefito.infrastructure.external;

import java.time.LocalDate;

/**
 * 외부 소스(K-Startup, 고용24, Q-Net, 장학재단 등)에서 수집한 공고를 표현하는 공통 모델.
 * 각 ExternalPostingSource 구현체는 외부 응답 DTO를 이 모델로 변환해서 돌려주고,
 * PostingMapper는 이 모델만 알면 되므로 소스가 늘어나도 이후 파이프라인은 그대로 재사용된다.
 *
 * category/region은 원본 문자열을 그대로 보존한다(Code 매핑은 PostingMapper의 책임).
 * startDate는 Posting 엔티티에 대응 컬럼이 없어 PostingMapper가 사용하지 않는다(저장되지 않음).
 * 그래도 소스가 시작일 개념을 갖고 있다면 이 모델을 통해 전달만 해두면, 나중에 Posting에
 * 컬럼이 추가될 때 소스 쪽 변경 없이 바로 활용할 수 있다. 시작일 개념이 없는 소스는 null로 둔다.
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
        LocalDate deadline
) {
}
