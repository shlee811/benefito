package com.lion.benefito.infrastructure.ai.embedding;

import java.util.List;

public record OpenAiEmbeddingResponse(List<EmbeddingData> data) {

    public record EmbeddingData(List<Float> embedding, int index) {
    }
}
