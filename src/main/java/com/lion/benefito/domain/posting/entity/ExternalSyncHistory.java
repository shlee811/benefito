package com.lion.benefito.domain.posting.entity;

import com.lion.benefito.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 외부 소스 동기화(initialSync/incrementalSync) 1회 실행 결과를 기록한다.
// lastSuccessfulSyncAt은 실행 자체가 끝까지 성공했을 때만 채워지고, 실패하면 null로 남아
// 다음 번 incrementalSync가 잘못된 기준 시각으로 넘어가지 않도록 한다.
@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "external_sync_history")
public class ExternalSyncHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "last_successful_sync_at")
    private LocalDateTime lastSuccessfulSyncAt;

    @Column(name = "fetched_count", nullable = false)
    private int fetchedCount;

    @Column(name = "created_count", nullable = false)
    private int createdCount;

    @Column(name = "updated_count", nullable = false)
    private int updatedCount;

    @Column(name = "unchanged_count", nullable = false)
    private int unchangedCount;

    // 기존 행이 있는 테이블에 NOT NULL 컬럼을 추가하는 것이라 DEFAULT를 명시해야 한다(Posting.status와 동일한 이유).
    @Column(name = "closed_count", nullable = false, columnDefinition = "integer default 0")
    private int closedCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;
}
