package com.lion.benefito.infrastructure.ai.rag;

import com.lion.benefito.domain.posting.dto.PostingSimilarityResponse;
import com.lion.benefito.domain.posting.repository.PostingRepository;
import com.lion.benefito.infrastructure.ai.embedding.EmbeddingClient;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PgVectorPostingRetriever implements PostingRetriever {

    private final PostingRepository postingRepository;
    private final EmbeddingClient embeddingClient;

    @Override
    public List<PostingSimilarityResponse> findSimilarPostings(String query, int topK) {
        if (topK < 1) {
            throw new RagException(RagErrorCode.INVALID_TOP_K);
        }

        // query의 null/blank 검증은 EmbeddingClient.embed()가 이미 수행한다 (EmbeddingErrorCode.INVALID_INPUT).
        List<Float> queryVector = embeddingClient.embed(query);
        String vectorLiteral = toVectorLiteral(queryVector);

        return postingRepository.findSimilarPostings(vectorLiteral, topK).stream()
                .map(PostingSimilarityResponse::from)
                .toList();
    }

    private String toVectorLiteral(List<Float> vector) {
        return vector.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }
}
