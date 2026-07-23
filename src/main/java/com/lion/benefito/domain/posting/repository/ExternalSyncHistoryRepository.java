package com.lion.benefito.domain.posting.repository;

import com.lion.benefito.domain.posting.entity.ExternalSyncHistory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalSyncHistoryRepository extends JpaRepository<ExternalSyncHistory, Long> {

    // incrementalSync에 넘길 기준 시각을 찾을 때 사용한다. 실패한 실행은 lastSuccessfulSyncAt이 null이라
    // 자동으로 제외된다.
    Optional<ExternalSyncHistory> findTopBySourceTypeAndLastSuccessfulSyncAtIsNotNullOrderByLastSuccessfulSyncAtDesc(
            String sourceType);
}
