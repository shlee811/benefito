package com.lion.benefito.domain.posting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lion.benefito.domain.posting.dto.ExternalPostingImportResponse;
import com.lion.benefito.domain.posting.entity.ExternalSyncHistory;
import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.domain.posting.entity.PostingStatus;
import com.lion.benefito.domain.posting.mapper.PostingMapper;
import com.lion.benefito.domain.posting.repository.ExternalSyncHistoryRepository;
import com.lion.benefito.domain.posting.repository.PostingRepository;
import com.lion.benefito.infrastructure.external.ExternalPostingData;
import com.lion.benefito.infrastructure.external.ExternalPostingSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SuppressWarnings("unchecked")
class ExternalPostingIngestServiceTest {

    private PostingRepository postingRepository;
    private PostingMapper postingMapper;
    private PostingEmbeddingService postingEmbeddingService;
    private ExternalSyncHistoryRepository externalSyncHistoryRepository;
    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOperations;
    private ExternalPostingIngestService ingestService;

    @BeforeEach
    void setUp() {
        postingRepository = mock(PostingRepository.class);
        postingMapper = mock(PostingMapper.class);
        postingEmbeddingService = mock(PostingEmbeddingService.class);
        externalSyncHistoryRepository = mock(ExternalSyncHistoryRepository.class);
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        ingestService = new ExternalPostingIngestService(
                postingRepository, postingMapper, postingEmbeddingService, externalSyncHistoryRepository,
                redisTemplate);
    }

    private ExternalPostingData data(String externalId) {
        return new ExternalPostingData("KSTARTUP", externalId, "제목" + externalId, "내용", "회사",
                "https://k-startup.go.kr/" + externalId, "사업화", "서울", "누구나", null, LocalDate.of(2026, 8, 3));
    }

    private Posting posting(Long id) {
        Posting posting = mock(Posting.class);
        when(posting.getId()).thenReturn(id);
        return posting;
    }

    @Test
    void 신규_공고는_저장되고_임베딩이_생성된다() {
        ExternalPostingData newData = data("1");
        ExternalPostingSource source = () -> List.of(newData);
        Posting saved = posting(1L);

        when(postingRepository.findBySourceTypeAndExternalId("KSTARTUP", "1")).thenReturn(Optional.empty());
        when(postingMapper.toNewPosting(newData)).thenReturn(saved);

        ExternalPostingImportResponse result = ingestService.importFrom(source);

        verify(postingRepository).save(saved);
        verify(postingEmbeddingService).refreshEmbedding(saved);
        assertThat(result.fetchedCount()).isEqualTo(1);
        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.updatedCount()).isEqualTo(0);
        assertThat(result.unchangedCount()).isEqualTo(0);
        assertThat(result.failedCount()).isEqualTo(0);
        assertThat(result.embeddingSuccessCount()).isEqualTo(1);
        assertThat(result.embeddingFailedCount()).isEqualTo(0);
    }

    @Test
    void 기존_공고와_동일하면_저장도_임베딩_호출도_하지_않는다() {
        ExternalPostingData unchangedData = data("1");
        ExternalPostingSource source = () -> List.of(unchangedData);
        Posting existing = posting(1L);

        when(postingRepository.findBySourceTypeAndExternalId("KSTARTUP", "1")).thenReturn(Optional.of(existing));
        when(postingMapper.applyChanges(existing, unchangedData)).thenReturn(false);

        ExternalPostingImportResponse result = ingestService.importFrom(source);

        verify(postingRepository, never()).save(any());
        verify(postingEmbeddingService, never()).refreshEmbedding(any());
        assertThat(result.unchangedCount()).isEqualTo(1);
        assertThat(result.updatedCount()).isEqualTo(0);
        assertThat(result.embeddingSuccessCount()).isEqualTo(0);
    }

    @Test
    void 기존_공고와_다르면_저장되고_임베딩이_재생성된다() {
        ExternalPostingData changedData = data("1");
        ExternalPostingSource source = () -> List.of(changedData);
        Posting existing = posting(1L);

        when(postingRepository.findBySourceTypeAndExternalId("KSTARTUP", "1")).thenReturn(Optional.of(existing));
        when(postingMapper.applyChanges(existing, changedData)).thenReturn(true);

        ExternalPostingImportResponse result = ingestService.importFrom(source);

        verify(postingRepository).save(existing);
        verify(postingEmbeddingService).refreshEmbedding(existing);
        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(result.createdCount()).isEqualTo(0);
        assertThat(result.embeddingSuccessCount()).isEqualTo(1);
    }

    @Test
    void 한_건의_저장이_실패해도_나머지_항목은_계속_처리된다() {
        ExternalPostingData failing = data("1");
        ExternalPostingData ok = data("2");
        ExternalPostingSource source = () -> List.of(failing, ok);
        Posting okPosting = posting(2L);

        when(postingRepository.findBySourceTypeAndExternalId("KSTARTUP", "1")).thenReturn(Optional.empty());
        when(postingMapper.toNewPosting(failing)).thenThrow(new RuntimeException("DB 제약 위반"));
        when(postingRepository.findBySourceTypeAndExternalId("KSTARTUP", "2")).thenReturn(Optional.empty());
        when(postingMapper.toNewPosting(ok)).thenReturn(okPosting);

        ExternalPostingImportResponse result = ingestService.importFrom(source);

        assertThat(result.fetchedCount()).isEqualTo(2);
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.createdCount()).isEqualTo(1);
        verify(postingEmbeddingService).refreshEmbedding(okPosting);
    }

    @Test
    void 임베딩_생성이_실패해도_저장_결과는_유지되고_실패_카운트만_증가한다() {
        ExternalPostingData newData = data("1");
        ExternalPostingSource source = () -> List.of(newData);
        Posting saved = posting(1L);

        when(postingRepository.findBySourceTypeAndExternalId("KSTARTUP", "1")).thenReturn(Optional.empty());
        when(postingMapper.toNewPosting(newData)).thenReturn(saved);
        doThrow(new RuntimeException("OpenAI 호출 실패")).when(postingEmbeddingService).refreshEmbedding(saved);

        ExternalPostingImportResponse result = ingestService.importFrom(source);

        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.embeddingSuccessCount()).isEqualTo(0);
        assertThat(result.embeddingFailedCount()).isEqualTo(1);
    }

    @Test
    void 수집_결과가_없으면_모든_카운트가_0이다() {
        ExternalPostingSource source = List::of;

        ExternalPostingImportResponse result = ingestService.importFrom(source);

        assertThat(result.fetchedCount()).isEqualTo(0);
        assertThat(result.createdCount()).isEqualTo(0);
        assertThat(result.updatedCount()).isEqualTo(0);
        assertThat(result.unchangedCount()).isEqualTo(0);
        assertThat(result.failedCount()).isEqualTo(0);
        assertThat(result.embeddingSuccessCount()).isEqualTo(0);
        assertThat(result.embeddingFailedCount()).isEqualTo(0);
        verify(postingRepository, never()).save(any());
    }

    @Test
    void 이번_수집_목록에_없는_기존_활성_공고는_CLOSED로_전환된다() {
        ExternalPostingData onlyItem = data("1");
        ExternalPostingSource source = () -> List.of(onlyItem);
        Posting saved = posting(1L);
        when(postingRepository.findBySourceTypeAndExternalId("KSTARTUP", "1")).thenReturn(Optional.empty());
        when(postingMapper.toNewPosting(onlyItem)).thenReturn(saved);
        when(postingRepository.closeMissingFromSource(
                "KSTARTUP", List.of("1"), PostingStatus.ACTIVE, PostingStatus.CLOSED)).thenReturn(3);

        ExternalPostingImportResponse result = ingestService.importFrom(source);

        assertThat(result.closedCount()).isEqualTo(3);
        verify(postingRepository).closeMissingFromSource(
                "KSTARTUP", List.of("1"), PostingStatus.ACTIVE, PostingStatus.CLOSED);
    }

    @Test
    void 수집_결과가_비어있으면_기존_공고를_CLOSED로_전환하지_않는다() {
        ExternalPostingSource source = List::of;

        ExternalPostingImportResponse result = ingestService.importFrom(source);

        assertThat(result.closedCount()).isEqualTo(0);
        verify(postingRepository, never()).closeMissingFromSource(any(), any(), any(), any());
    }

    @Test
    void closeExpiredPostings는_postingRepository의_결과를_그대로_반환한다() {
        when(postingRepository.closeExpiredPostings(any(), eq(PostingStatus.ACTIVE), eq(PostingStatus.CLOSED)))
                .thenReturn(7);

        int closed = ingestService.closeExpiredPostings();

        assertThat(closed).isEqualTo(7);
    }

    @Test
    void sync는_직전_성공_이력이_없으면_initialSync로_동작한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("external-sync:lock:KSTARTUP"), any(), any(java.time.Duration.class))).thenReturn(true);
        when(externalSyncHistoryRepository
                .findTopBySourceTypeAndLastSuccessfulSyncAtIsNotNullOrderByLastSuccessfulSyncAtDesc("KSTARTUP"))
                .thenReturn(Optional.empty());
        ExternalPostingSource source = List::of;

        ingestService.sync("KSTARTUP", source);

        ArgumentCaptor<ExternalSyncHistory> captor = ArgumentCaptor.forClass(ExternalSyncHistory.class);
        verify(externalSyncHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getLastSuccessfulSyncAt()).isNotNull();
        verify(redisTemplate).delete("external-sync:lock:KSTARTUP");
    }

    @Test
    void sync는_직전_성공_이력이_있으면_incrementalSync로_동작하며_이전_시각을_전달한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("external-sync:lock:KSTARTUP"), any(), any(java.time.Duration.class))).thenReturn(true);
        LocalDateTime previousSync = LocalDateTime.of(2026, 1, 1, 0, 0);
        ExternalSyncHistory previousHistory = ExternalSyncHistory.builder()
                .sourceType("KSTARTUP")
                .startedAt(previousSync)
                .finishedAt(previousSync)
                .lastSuccessfulSyncAt(previousSync)
                .fetchedCount(0).createdCount(0).updatedCount(0).unchangedCount(0).failedCount(0)
                .build();
        when(externalSyncHistoryRepository
                .findTopBySourceTypeAndLastSuccessfulSyncAtIsNotNullOrderByLastSuccessfulSyncAtDesc("KSTARTUP"))
                .thenReturn(Optional.of(previousHistory));
        ExternalPostingSource source = List::of;

        ingestService.sync("KSTARTUP", source);

        verify(externalSyncHistoryRepository).save(any());
        verify(redisTemplate).delete("external-sync:lock:KSTARTUP");
    }

    @Test
    void 락을_획득하지_못하면_동기화를_건너뛰고_아무_저장소도_건드리지_않는다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("external-sync:lock:KSTARTUP"), any(), any(java.time.Duration.class))).thenReturn(false);
        ExternalPostingSource source = List::of;

        ExternalPostingImportResponse result = ingestService.sync("KSTARTUP", source);

        assertThat(result.fetchedCount()).isEqualTo(0);
        verify(externalSyncHistoryRepository, never()).save(any());
        verify(externalSyncHistoryRepository, never())
                .findTopBySourceTypeAndLastSuccessfulSyncAtIsNotNullOrderByLastSuccessfulSyncAtDesc(any());
    }

    @Test
    void initialSync는_lastSuccessfulSyncAt이_null인_sync_history를_남기고_성공하면_현재시각으로_채운다() {
        ExternalPostingData newData = data("1");
        ExternalPostingSource source = () -> List.of(newData);
        Posting saved = posting(1L);
        when(postingRepository.findBySourceTypeAndExternalId("KSTARTUP", "1")).thenReturn(Optional.empty());
        when(postingMapper.toNewPosting(newData)).thenReturn(saved);

        ExternalPostingImportResponse result = ingestService.initialSync("KSTARTUP", source);

        assertThat(result.createdCount()).isEqualTo(1);
        ArgumentCaptor<ExternalSyncHistory> captor = ArgumentCaptor.forClass(ExternalSyncHistory.class);
        verify(externalSyncHistoryRepository).save(captor.capture());
        ExternalSyncHistory history = captor.getValue();
        assertThat(history.getSourceType()).isEqualTo("KSTARTUP");
        assertThat(history.getStartedAt()).isNotNull();
        assertThat(history.getFinishedAt()).isNotNull();
        assertThat(history.getLastSuccessfulSyncAt()).isNotNull();
        assertThat(history.getFetchedCount()).isEqualTo(1);
        assertThat(history.getCreatedCount()).isEqualTo(1);
    }

    @Test
    void incrementalSync는_전달받은_이전_성공시각과_무관하게_동일한_수집_저장_로직을_수행한다() {
        ExternalPostingData unchangedData = data("1");
        ExternalPostingSource source = () -> List.of(unchangedData);
        Posting existing = posting(1L);
        when(postingRepository.findBySourceTypeAndExternalId("KSTARTUP", "1")).thenReturn(Optional.of(existing));
        when(postingMapper.applyChanges(existing, unchangedData)).thenReturn(false);
        LocalDateTime previousSync = LocalDateTime.of(2026, 1, 1, 0, 0);

        ExternalPostingImportResponse result = ingestService.incrementalSync("KSTARTUP", source, previousSync);

        assertThat(result.unchangedCount()).isEqualTo(1);
        ArgumentCaptor<ExternalSyncHistory> captor = ArgumentCaptor.forClass(ExternalSyncHistory.class);
        verify(externalSyncHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getLastSuccessfulSyncAt()).isNotNull();
        assertThat(captor.getValue().getUnchangedCount()).isEqualTo(1);
    }

    @Test
    void 동기화_실행_자체가_실패하면_lastSuccessfulSyncAt이_null인_sync_history를_남기고_예외를_다시_던진다() {
        ExternalPostingSource source = () -> {
            throw new RuntimeException("외부 API 응답 파싱 실패");
        };

        assertThatThrownBy(() -> ingestService.initialSync("QNET", source))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("외부 API 응답 파싱 실패");

        ArgumentCaptor<ExternalSyncHistory> captor = ArgumentCaptor.forClass(ExternalSyncHistory.class);
        verify(externalSyncHistoryRepository).save(captor.capture());
        ExternalSyncHistory history = captor.getValue();
        assertThat(history.getSourceType()).isEqualTo("QNET");
        assertThat(history.getLastSuccessfulSyncAt()).isNull();
        assertThat(history.getFetchedCount()).isEqualTo(0);
    }
}
