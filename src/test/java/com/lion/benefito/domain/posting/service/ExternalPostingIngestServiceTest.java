package com.lion.benefito.domain.posting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lion.benefito.domain.posting.dto.ExternalPostingImportResponse;
import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.domain.posting.mapper.PostingMapper;
import com.lion.benefito.domain.posting.repository.PostingRepository;
import com.lion.benefito.infrastructure.external.ExternalPostingData;
import com.lion.benefito.infrastructure.external.ExternalPostingSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExternalPostingIngestServiceTest {

    private PostingRepository postingRepository;
    private PostingMapper postingMapper;
    private PostingEmbeddingService postingEmbeddingService;
    private ExternalPostingIngestService ingestService;

    @BeforeEach
    void setUp() {
        postingRepository = mock(PostingRepository.class);
        postingMapper = mock(PostingMapper.class);
        postingEmbeddingService = mock(PostingEmbeddingService.class);
        ingestService = new ExternalPostingIngestService(postingRepository, postingMapper, postingEmbeddingService);
    }

    private ExternalPostingData data(String externalId) {
        return new ExternalPostingData("KSTARTUP", externalId, "제목" + externalId, "내용", "회사",
                "https://k-startup.go.kr/" + externalId, "사업화", "서울", "누구나", LocalDate.of(2026, 8, 3));
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
}
