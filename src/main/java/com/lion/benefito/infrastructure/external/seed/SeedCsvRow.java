package com.lion.benefito.infrastructure.external.seed;

// CSV 컬럼 순서 그대로: title, organization, category, target, qualification, content, benefit
public record SeedCsvRow(
        String title,
        String organization,
        String category,
        String target,
        String qualification,
        String content,
        String benefit
) {
}
