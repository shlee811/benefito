package com.lion.benefito.infrastructure.external;

import java.util.List;

/**
 * 외부 데이터 소스로부터 공고를 수집하는 공통 인터페이스.
 * 페이징/수집 범위 제한 등 소스별 세부 구현은 각 구현체(KStartupPostingSource 등) 내부에 캡슐화하고,
 * 호출하는 쪽(ExternalPostingIngestService)은 완성된 목록만 받는다.
 */
public interface ExternalPostingSource {

    List<ExternalPostingData> fetchPostings();
}
