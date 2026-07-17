package com.lion.benefito.infrastructure.ai.rag;

import com.lion.benefito.domain.posting.entity.Posting;
import java.util.List;

public interface PostingRetriever {

    // TODO: 다음 PR에서 pgvector 기반 유사 공고 검색 구현 예정. 이번 PR에서는 구현체/Bean 등록 없음.
    List<Posting> findSimilarPostings(String query, int topK);
}
