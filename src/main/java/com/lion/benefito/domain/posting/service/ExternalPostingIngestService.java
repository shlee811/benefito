package com.lion.benefito.domain.posting.service;

import com.lion.benefito.domain.posting.dto.ExternalPostingImportResponse;
import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.domain.posting.mapper.PostingMapper;
import com.lion.benefito.domain.posting.repository.PostingRepository;
import com.lion.benefito.infrastructure.external.ExternalPostingData;
import com.lion.benefito.infrastructure.external.ExternalPostingSource;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final PostingRepository postingRepository;
    private final PostingMapper postingMapper;
    private final PostingEmbeddingService postingEmbeddingService;

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

        return new ExternalPostingImportResponse(fetched.size(), created, updated, unchanged, failed,
                embeddingSuccess, embeddingFailed);
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

    private record SaveOutcome(Posting posting, ChangeType changeType) {
    }

    private enum ChangeType {
        CREATED, UPDATED, UNCHANGED
    }
}
