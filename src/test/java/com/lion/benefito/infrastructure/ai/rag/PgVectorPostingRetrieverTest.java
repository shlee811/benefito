package com.lion.benefito.infrastructure.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lion.benefito.domain.posting.dto.PostingSimilarityResponse;
import com.lion.benefito.domain.posting.repository.PostingRepository;
import com.lion.benefito.domain.posting.repository.PostingSimilarityProjection;
import com.lion.benefito.infrastructure.ai.embedding.EmbeddingClient;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PgVectorPostingRetrieverTest {

    @Mock
    private PostingRepository postingRepository;

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private PostingSimilarityProjection projection;

    private PgVectorPostingRetriever retriever() {
        return new PgVectorPostingRetriever(postingRepository, embeddingClient);
    }

    @Test
    void 정상_조회시_distance를_similarity로_변환한다() {
        when(embeddingClient.embed("서울 지역 장학금")).thenReturn(List.of(0.1f, 0.2f, 0.3f));
        when(projection.getId()).thenReturn(1L);
        when(projection.getTitle()).thenReturn("국가장학금 안내");
        when(projection.getContent()).thenReturn("소득분위 기준 장학금 지원");
        when(projection.getCategoryCode()).thenReturn("SCHOLARSHIP");
        when(projection.getSourceUrl()).thenReturn("https://example.com/1");
        when(projection.getDistance()).thenReturn(0.2);
        when(postingRepository.findSimilarPostings("[0.1,0.2,0.3]", 5)).thenReturn(List.of(projection));

        List<PostingSimilarityResponse> result = retriever().findSimilarPostings("서울 지역 장학금", 5);

        assertThat(result).hasSize(1);
        PostingSimilarityResponse response = result.get(0);
        assertThat(response.postingId()).isEqualTo(1L);
        assertThat(response.category()).isEqualTo("SCHOLARSHIP");
        assertThat(response.similarity()).isEqualTo(0.8); // 1.0 - 0.2
    }

    @Test
    void topK가_1_미만이면_임베딩_호출_전에_예외를_던진다() {
        assertThatThrownBy(() -> retriever().findSimilarPostings("질문", 0))
                .isInstanceOf(RagException.class)
                .satisfies(ex -> assertThat(((RagException) ex).getCode())
                        .isEqualTo(RagErrorCode.INVALID_TOP_K));

        verifyNoInteractions(embeddingClient, postingRepository);
    }

    @Test
    void topK가_음수여도_예외를_던진다() {
        assertThatThrownBy(() -> retriever().findSimilarPostings("질문", -1))
                .isInstanceOf(RagException.class);

        verifyNoInteractions(embeddingClient, postingRepository);
    }

    @Test
    void 결과가_없으면_빈_리스트를_반환한다() {
        when(embeddingClient.embed(anyString())).thenReturn(List.of(0.5f));
        when(postingRepository.findSimilarPostings(anyString(), anyInt())).thenReturn(List.of());

        List<PostingSimilarityResponse> result = retriever().findSimilarPostings("아무도 없는 주제", 3);

        assertThat(result).isEmpty();
    }

    @Test
    void 쿼리_벡터_문자열이_repository로_그대로_전달된다() {
        when(embeddingClient.embed("질문")).thenReturn(List.of(1.0f, -2.5f));
        when(postingRepository.findSimilarPostings(anyString(), anyInt())).thenReturn(List.of());

        retriever().findSimilarPostings("질문", 10);

        verify(postingRepository).findSimilarPostings("[1.0,-2.5]", 10);
    }
}
