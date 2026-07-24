package com.lion.benefito.infrastructure.ai.rag;

import com.lion.benefito.domain.posting.dto.PostingSimilarityResponse;
import java.util.List;

public interface PostingRetriever {

    List<PostingSimilarityResponse> findSimilarPostings(String query, int topK);
}
