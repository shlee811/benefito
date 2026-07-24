package com.lion.benefito.infrastructure.external.scholarship;

// 실제로 매핑에 쓰는 컬럼만 담는다(운영기관구분/상품구분/학자금유형구분/대학구분/특정자격 상세내용/
// 선발방법/선발인원/자격제한/추천필요여부 등은 이번 매핑에 쓰지 않아 제외했다).
public record ScholarshipCsvRow(
        String no,
        String orgName,
        String productName,
        String gradeType,
        String majorType,
        String academicCriteria,
        String incomeCriteria,
        String supportDetail,
        String regionCondition,
        String requiredDocuments,
        String homepageUrl,
        String recruitStartDate,
        String recruitEndDate
) {
}
