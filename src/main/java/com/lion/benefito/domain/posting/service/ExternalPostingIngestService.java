package com.lion.benefito.domain.posting.service;

import com.lion.benefito.domain.posting.dto.ExternalPostingImportResponse;
import com.lion.benefito.domain.posting.entity.ExternalSyncHistory;
import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.domain.posting.entity.PostingStatus;
import com.lion.benefito.domain.posting.mapper.PostingMapper;
import com.lion.benefito.domain.posting.repository.ExternalSyncHistoryRepository;
import com.lion.benefito.domain.posting.repository.PostingRepository;
import com.lion.benefito.infrastructure.external.ExternalPostingData;
import com.lion.benefito.infrastructure.external.ExternalPostingSource;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 외부 소스에서 가져온 공고를 저장/갱신하고 임베딩을 재생성하는 공통 파이프라인.
 *
 * 트랜잭션 설계: 공고 저장/갱신(postingRepository.save)과 임베딩 생성(OpenAI API 호출 포함)을
 * 하나의 트랜잭션으로 묶지 않는다. save()는 Spring Data JPA가 건별로 이미 트랜잭션 처리를 해주고,
 * postingEmbeddingService.refreshEmbedding()은 별도 빈의 별도 @Transactional 메서드로 호출되므로
 * 외부 API 호출 + OpenAI 호출이 장시간 하나의 DB 트랜잭션에 물리는 일이 없다.
 *
 * 실패 격리: 항목 하나의 저장/임베딩 실패가 나머지 항목 처리를 막지 않도록 항목 단위로 try/catch한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalPostingIngestService {

    // 동일 소스 동기화가 겹쳐 실행되는 것을 막는 락의 최대 보유 시간. 실행이 이 시간을 넘기면 Redis가
    // 키를 자동 만료시켜, 프로세스가 비정상 종료되어도 락이 영구히 남지 않는다(데드락 방지).
    private static final Duration LOCK_TIMEOUT = Duration.ofMinutes(30);
    private static final String LOCK_KEY_PREFIX = "external-sync:lock:";

    private final PostingRepository postingRepository;
    private final PostingMapper postingMapper;
    private final PostingEmbeddingService postingEmbeddingService;
    private final ExternalSyncHistoryRepository externalSyncHistoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // 스케줄러/관리자 API 양쪽의 공통 진입점이다. 소스별 락을 획득한 뒤, 이 소스의 성공 이력이 있는지에
    // 따라 initialSync/incrementalSync 중 하나로 분기한다. 락을 획득하지 못하면(=같은 소스가 이미
    // 동기화 중이면) 아무 것도 하지 않고 건너뛴다.
    public ExternalPostingImportResponse sync(String sourceType, ExternalPostingSource source) {
        String lockKey = LOCK_KEY_PREFIX + sourceType;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TIMEOUT);
        if (!Boolean.TRUE.equals(acquired)) {
            log.info("{} 동기화가 이미 실행 중이라 이번 실행은 건너뜁니다.", sourceType);
            return new ExternalPostingImportResponse(0, 0, 0, 0, 0, 0, 0, 0);
        }

        try {
            Optional<ExternalSyncHistory> lastSuccess = externalSyncHistoryRepository
                    .findTopBySourceTypeAndLastSuccessfulSyncAtIsNotNullOrderByLastSuccessfulSyncAtDesc(sourceType);
            return lastSuccess.isEmpty()
                    ? initialSync(sourceType, source)
                    : incrementalSync(sourceType, source, lastSuccess.get().getLastSuccessfulSyncAt());
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    // DB에 해당 소스 데이터가 아직 없을 때(=이 소스의 성공 이력이 없을 때) 호출하는 최초 전체 수집이다.
    // 현재 어떤 외부 소스도 날짜 조건 조회를 지원하지 않으므로 importFrom()과 실제로 수행하는 일은 같지만,
    // sync history를 남긴다는 점에서 구분된다.
    public ExternalPostingImportResponse initialSync(String sourceType, ExternalPostingSource source) {
        return runWithHistory(sourceType, source, null);
    }

    // 정기 실행용 증분 수집이다. lastSuccessfulSyncAt은 현재는 소스 호출 자체에는 쓰이지 않지만
    // (날짜 조건 조회를 지원하는 소스가 생기면 그때 활용), sync history에는 항상 기록해 다음 번
    // 기준 시각을 추적할 수 있게 한다. 신규/변경/동일 판단은 initialSync와 동일하게 externalId +
    // contentHash 비교로 이뤄진다.
    public ExternalPostingImportResponse incrementalSync(
            String sourceType, ExternalPostingSource source, LocalDateTime lastSuccessfulSyncAt) {
        return runWithHistory(sourceType, source, lastSuccessfulSyncAt);
    }

    private ExternalPostingImportResponse runWithHistory(
            String sourceType, ExternalPostingSource source, LocalDateTime previousSyncAt) {
        LocalDateTime startedAt = LocalDateTime.now();
        log.info("{} 동기화를 시작합니다(이전 성공 시각={}).", sourceType, previousSyncAt);

        ExternalPostingImportResponse response;
        try {
            response = importFrom(source);
        } catch (Exception e) {
            log.warn("{} 동기화 실행 자체가 실패했습니다: {}", sourceType, e.getMessage());
            saveHistory(sourceType, startedAt, new ExternalPostingImportResponse(0, 0, 0, 0, 0, 0, 0, 0), false);
            throw e;
        }

        saveHistory(sourceType, startedAt, response, true);
        return response;
    }

    // succeeded가 true일 때만 lastSuccessfulSyncAt을 채운다(실패 시 null로 남겨 다음 incrementalSync가
    // 잘못된 기준 시각을 받지 않도록 한다).
    private void saveHistory(String sourceType, LocalDateTime startedAt,
                              ExternalPostingImportResponse response, boolean succeeded) {
        LocalDateTime finishedAt = LocalDateTime.now();
        externalSyncHistoryRepository.save(ExternalSyncHistory.builder()
                .sourceType(sourceType)
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .lastSuccessfulSyncAt(succeeded ? finishedAt : null)
                .fetchedCount(response.fetchedCount())
                .createdCount(response.createdCount())
                .updatedCount(response.updatedCount())
                .unchangedCount(response.unchangedCount())
                .closedCount(response.closedCount())
                .failedCount(response.failedCount())
                .build());
        log.info("{} 동기화 결과: fetched={}, created={}, updated={}, unchanged={}, closed={}, failed={}",
                sourceType, response.fetchedCount(), response.createdCount(), response.updatedCount(),
                response.unchangedCount(), response.closedCount(), response.failedCount());
    }

    public ExternalPostingImportResponse importFrom(ExternalPostingSource source) {
        List<ExternalPostingData> fetched = source.fetchPostings();
        log.info("{}에서 공고 {}건을 수집했습니다.", source.getClass().getSimpleName(), fetched.size());

        int created = 0;
        int updated = 0;
        int unchanged = 0;
        int failed = 0;
        int embeddingSuccess = 0;
        int embeddingFailed = 0;

        for (ExternalPostingData data : fetched) {
            SaveOutcome outcome;
            try {
                outcome = saveOrUpdate(data);
            } catch (Exception e) {
                log.warn("공고 저장 실패(sourceType={}, externalId={}): {}",
                        data.sourceType(), data.externalId(), e.getMessage());
                failed++;
                continue;
            }

            switch (outcome.changeType()) {
                case CREATED -> created++;
                case UPDATED -> updated++;
                case UNCHANGED -> unchanged++;
            }

            if (outcome.changeType() == ChangeType.UNCHANGED) {
                continue;
            }

            try {
                postingEmbeddingService.refreshEmbedding(outcome.posting());
                embeddingSuccess++;
            } catch (Exception e) {
                log.warn("공고 임베딩 생성 실패(postingId={}): {}", outcome.posting().getId(), e.getMessage());
                embeddingFailed++;
            }
        }

        int closed = closeMissingFromSource(fetched);

        return new ExternalPostingImportResponse(fetched.size(), created, updated, unchanged, failed,
                embeddingSuccess, embeddingFailed, closed);
    }

    // 이번에 수집된 externalId 목록에 없는 기존 ACTIVE 공고를 CLOSED로 전환한다. fetched가 비어 있으면
    // (일시적 API 실패 등으로 이번 수집 결과 자체가 없는 경우) 기존 공고를 전부 닫아버리는 것을 막기 위해
    // 아예 호출하지 않는다.
    private int closeMissingFromSource(List<ExternalPostingData> fetched) {
        if (fetched.isEmpty()) {
            return 0;
        }
        String sourceType = fetched.get(0).sourceType();
        List<String> fetchedExternalIds = fetched.stream().map(ExternalPostingData::externalId).toList();
        return postingRepository.closeMissingFromSource(
                sourceType, fetchedExternalIds, PostingStatus.ACTIVE, PostingStatus.CLOSED);
    }

    private SaveOutcome saveOrUpdate(ExternalPostingData data) {
        Optional<Posting> existing =
                postingRepository.findBySourceTypeAndExternalId(data.sourceType(), data.externalId());

        if (existing.isEmpty()) {
            Posting posting = postingMapper.toNewPosting(data);
            postingRepository.save(posting);
            return new SaveOutcome(posting, ChangeType.CREATED);
        }

        Posting posting = existing.get();
        boolean changed = postingMapper.applyChanges(posting, data);
        if (changed) {
            postingRepository.save(posting);
            return new SaveOutcome(posting, ChangeType.UPDATED);
        }
        return new SaveOutcome(posting, ChangeType.UNCHANGED);
    }

    // 소스 구분 없이, 마감일이 지났는데 아직 ACTIVE인 공고를 전부 CLOSED로 전환한다.
    // contentHash가 그대로라 재수집 때 건드려지지 않는 공고도 마감일이 지나면 여기서 잡힌다.
    public int closeExpiredPostings() {
        int closed = postingRepository.closeExpiredPostings(
                LocalDate.now(), PostingStatus.ACTIVE, PostingStatus.CLOSED);
        log.info("마감일이 지난 공고 {}건을 CLOSED로 전환했습니다.", closed);
        return closed;
    }

    private record SaveOutcome(Posting posting, ChangeType changeType) {
    }

    private enum ChangeType {
        CREATED, UPDATED, UNCHANGED
    }
}
