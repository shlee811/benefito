package com.lion.benefito.infrastructure.external.seed;

// RAG 테스트용 시드 CSV 컬럼 순서 그대로: title, organization, region, target, major, grade,
// benefit, applicationMethod, content, regStart, deadline, salary, employmentType, enterpriseType,
// career, education.
// 기존 SeedCsvRow(공모전/대외활동용, 7컬럼)와 달리 region/regStart/deadline이 있어야 지역·마감일
// 다양성 요구사항을 채울 수 있어 별도 스키마로 둔다. salary/employmentType/enterpriseType/career/
// education은 JOB 카테고리 상세 화면 표시용으로만 쓰이고, 그 외 카테고리 CSV에서는 빈 값으로 둔다.
public record RagSeedCsvRow(
        String title,
        String organization,
        String region,
        String target,
        String major,
        String grade,
        String benefit,
        String applicationMethod,
        String content,
        String regStart,
        String deadline,
        String salary,
        String employmentType,
        String enterpriseType,
        String career,
        String education
) {
}
