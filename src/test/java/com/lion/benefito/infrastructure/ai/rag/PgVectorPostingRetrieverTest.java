package com.lion.benefito.infrastructure.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.code.entity.CommonCodeType;
import com.lion.benefito.domain.code.repository.CodeRepository;
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
    private CodeRepository codeRepository;

    @Mock
    private PostingSimilarityProjection projection;

    private PgVectorPostingRetriever retriever() {
        return new PgVectorPostingRetriever(postingRepository, embeddingClient, codeRepository);
    }

    // 필터링에 쓰이는 getName()만 매 카테고리마다 실제로 호출되고, getCode()는 최종 매칭된 카테고리에서만
    // 호출되므로 lenient로 stub 한다(그렇지 않으면 매칭되지 않은 카테고리의 getCode() stub이
    // UnnecessaryStubbingException을 유발한다).
    private Code categoryCode(String code, String name) {
        Code mock = org.mockito.Mockito.mock(Code.class);
        org.mockito.Mockito.lenient().when(mock.getCode()).thenReturn(code);
        when(mock.getName()).thenReturn(name);
        return mock;
    }

    private void noCategoriesSeeded() {
        when(codeRepository.findByCodeTypeAndIsActiveTrueOrderByDisplayOrderAsc(CommonCodeType.CATEGORY))
                .thenReturn(List.of());
    }

    @Test
    void 정상_조회시_distance를_similarity로_변환한다() {
        noCategoriesSeeded();
        when(embeddingClient.embed("서울 지역 장학금 안내")).thenReturn(List.of(0.1f, 0.2f, 0.3f));
        when(projection.getId()).thenReturn(1L);
        when(projection.getTitle()).thenReturn("국가장학금 안내");
        when(projection.getContent()).thenReturn("소득분위 기준 장학금 지원");
        when(projection.getCategoryCode()).thenReturn("SCHOLARSHIP");
        when(projection.getSourceUrl()).thenReturn("https://example.com/1");
        when(projection.getDistance()).thenReturn(0.2);
        when(postingRepository.findSimilarPostings("[0.1,0.2,0.3]", 5)).thenReturn(List.of(projection));

        List<PostingSimilarityResponse> result = retriever().findSimilarPostings("서울 지역 장학금 안내", 5);

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

        verifyNoInteractions(embeddingClient, postingRepository, codeRepository);
    }

    @Test
    void topK가_음수여도_예외를_던진다() {
        assertThatThrownBy(() -> retriever().findSimilarPostings("질문", -1))
                .isInstanceOf(RagException.class);

        verifyNoInteractions(embeddingClient, postingRepository, codeRepository);
    }

    @Test
    void 결과가_없으면_빈_리스트를_반환한다() {
        noCategoriesSeeded();
        when(embeddingClient.embed(anyString())).thenReturn(List.of(0.5f));
        when(postingRepository.findSimilarPostings(anyString(), anyInt())).thenReturn(List.of());

        List<PostingSimilarityResponse> result = retriever().findSimilarPostings("아무도 없는 주제", 3);

        assertThat(result).isEmpty();
    }

    @Test
    void 쿼리_벡터_문자열이_repository로_그대로_전달된다() {
        noCategoriesSeeded();
        when(embeddingClient.embed("질문")).thenReturn(List.of(1.0f, -2.5f));
        when(postingRepository.findSimilarPostings(anyString(), anyInt())).thenReturn(List.of());

        retriever().findSimilarPostings("질문", 10);

        verify(postingRepository).findSimilarPostings("[1.0,-2.5]", 10);
    }

    @Test
    void 질문에_카테고리_표시명이_포함되면_해당_카테고리로_한정해서_검색한다() {
        Code scholarship = categoryCode("SCHOLARSHIP", "장학금");
        Code certificate = categoryCode("CERTIFICATE", "자격증");
        when(codeRepository.findByCodeTypeAndIsActiveTrueOrderByDisplayOrderAsc(CommonCodeType.CATEGORY))
                .thenReturn(List.of(scholarship, certificate));
        when(embeddingClient.embed("IT 자격증 추천해줘")).thenReturn(List.of(0.1f));
        when(postingRepository.findSimilarPostingsByCategory("[0.1]", "CERTIFICATE", 5)).thenReturn(List.of());

        retriever().findSimilarPostings("IT 자격증 추천해줘", 5);

        verify(postingRepository).findSimilarPostingsByCategory("[0.1]", "CERTIFICATE", 5);
        verify(postingRepository, never()).findSimilarPostings(anyString(), anyInt());
    }

    @Test
    void 질문에_카테고리_표시명이_없으면_카테고리_제한없이_검색한다() {
        noCategoriesSeeded();
        when(embeddingClient.embed("나한테 맞는 혜택 추천해줘")).thenReturn(List.of(0.1f));
        when(postingRepository.findSimilarPostings("[0.1]", 5)).thenReturn(List.of());

        retriever().findSimilarPostings("나한테 맞는 혜택 추천해줘", 5);

        verify(postingRepository).findSimilarPostings("[0.1]", 5);
        verify(postingRepository, never()).findSimilarPostingsByCategory(anyString(), anyString(), anyInt());
    }
}
