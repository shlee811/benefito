package com.lion.benefito.domain.posting.dto;

public record ExternalPostingImportResponse(
        int fetchedCount,
        int createdCount,
        int updatedCount,
        int unchangedCount,
        int failedCount,
        int embeddingSuccessCount,
        int embeddingFailedCount,
        int closedCount
) {
}
