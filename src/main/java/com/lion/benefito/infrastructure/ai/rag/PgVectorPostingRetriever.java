package com.lion.benefito.infrastructure.ai.rag;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.code.entity.CommonCodeType;
import com.lion.benefito.domain.code.repository.CodeRepository;
import com.lion.benefito.domain.posting.dto.PostingSimilarityResponse;
import com.lion.benefito.domain.posting.repository.PostingRepository;
import com.lion.benefito.domain.posting.repository.PostingSimilarityProjection;
import com.lion.benefito.infrastructure.ai.embedding.EmbeddingClient;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PgVectorPostingRetriever implements PostingRetriever {

    private final PostingRepository postingRepository;
    private final EmbeddingClient embeddingClient;
    private final CodeRepository codeRepository;

    @Override
    public List<PostingSimilarityResponse> findSimilarPostings(String query, int topK) {
        if (topK < 1) {
            throw new RagException(RagErrorCode.INVALID_TOP_K);
        }

        // query의 null/blank 검증은 EmbeddingClient.embed()가 이미 수행한다 (EmbeddingErrorCode.INVALID_INPUT).
        List<Float> queryVector = embeddingClient.embed(query);
        String vectorLiteral = toVectorLiteral(queryVector);

        Optional<String> detectedCategory = detectCategory(query);
        List<PostingSimilarityProjection> projections = detectedCategory
                .map(categoryCode -> postingRepository.findSimilarPostingsByCategory(vectorLiteral, categoryCode, topK))
                .orElseGet(() -> postingRepository.findSimilarPostings(vectorLiteral, topK));

        return projections.stream()
                .map(PostingSimilarityResponse::from)
                .toList();
    }

    // 질문 문자열에 카테고리 표시명(예: "자격증", "공모전")이 그대로 포함되어 있으면 해당 카테고리로 검색을
    // 한정한다. 표시명이 여러 개 겹치는 경우는 없지만(장학금/정부지원사업/공모전/대외활동/자격증/채용/인턴 중
    // 서로 포함 관계인 이름이 없음), 방어적으로 가장 긴 이름을 우선한다.
    private Optional<String> detectCategory(String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }

        return codeRepository.findByCodeTypeAndIsActiveTrueOrderByDisplayOrderAsc(CommonCodeType.CATEGORY).stream()
                .filter(code -> query.contains(code.getName()))
                .max(Comparator.comparing(code -> code.getName().length()))
                .map(Code::getCode);
    }

    private String toVectorLiteral(List<Float> vector) {
        return vector.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }
}
