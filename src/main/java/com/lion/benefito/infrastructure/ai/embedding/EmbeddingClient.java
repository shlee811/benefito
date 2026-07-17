package com.lion.benefito.infrastructure.ai.embedding;

import java.util.List;

public interface EmbeddingClient {

    // TODO: 다음 PR에서 OpenAI Embedding API 연동 구현 예정. 이번 PR에서는 구현체/Bean 등록 없음.
    List<Float> embed(String text);
}
