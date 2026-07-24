package com.lion.benefito.domain.posting.entity;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "postings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_postings_source_type_external_id",
                columnNames = {"source_type", "external_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Posting extends BaseTimeEntity {

    // "추천 공고" 기준: 로그인 사용자는 관심 카테고리 일치 여부를 우선으로 보고(PostingService의
    // 개인화 로직), 그 외에는 마감이 임박한(오늘부터 이 일수 이내) ACTIVE 공고를 기준으로 삼는다.
    // 비로그인 사용자나 관심사가 없는 사용자에게는 이 마감 임박 기준만 적용된다.
    public static final int RECOMMENDED_DEADLINE_WITHIN_DAYS = 7;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_code_id", nullable = false)
    private Code categoryCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_code_id")
    private Code regionCode;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDate endDate;

    // 원서접수 시작일 등 공고의 시작일 개념. Q-Net(원서접수 시작일) 도입 전에는 어떤 소스도 이 값을
    // 채우지 않아 컬럼 자체가 없었다(캘린더 START 집계는 그 시절 createdAt으로 임시 대체했었다).
    private LocalDate startDate;

    private String enterpriseType;

    private String employmentType;

    private String salary;

    @Column(columnDefinition = "TEXT")
    private String qualification;

    // JOB 카테고리 상세 화면 표시용 경력/학력 조건. 채용 성격의 소스만 채우고(그 외 카테고리는 개념이
    // 안 맞아 null), 기존 5개 컬럼(enterpriseType/employmentType/salary/qualification)과 동일하게
    // 자유 형식 텍스트로 둔다(값 종류가 "경력무관", "신입 또는 경력 1년 이하"처럼 문장형이라 enum으로
    // 제한하기 어렵다).
    private String career;

    private String education;

    private String homepageUrl;

    @Column(nullable = false)
    private long viewCount;

    // scraps 테이블의 실제 스크랩 수를 비정규화해 저장한 값. sort=popular 정렬 기준으로 쓰인다.
    // Specification+Pageable/Sort 조합에서는 집계(서브쿼리) 기준 정렬을 표현할 수 없어(Pageable의
    // Sort가 Specification 내부에서 설정한 orderBy를 덮어씀) viewCount와 동일한 방식으로 비정규화했다.
    // ScrapCommandServiceImpl의 스크랩 추가/삭제 시점에 increment/decrementScrapCount로만 갱신한다.
    @Column(name = "scrap_count", nullable = false, columnDefinition = "integer default 0")
    private long scrapCount;

    // title/content 기반 임베딩 벡터. PostingRepository.updateEmbedding() 벌크 쿼리로만 갱신한다(viewCount와 동일한 방식).
    // insertable/updatable=false: Hibernate가 이 컬럼을 INSERT/UPDATE SQL에 포함시키면 String 파라미터를
    // vector 컬럼에 바인딩하려다 "column embedding is of type vector but expression is of type character
    // varying" 오류가 난다(신규 Posting을 JPA save()로 insert할 때 실제로 재현됨). 항상 이 컬럼을 제외시키고
    // 신규 행은 DB 기본값(NULL)로 들어가게 한 뒤, 위 네이티브 쿼리(명시적 CAST 포함)로만 값을 채운다.
    @Column(columnDefinition = "vector(1536)", insertable = false, updatable = false)
    private String embedding;

    // 외부 수집 공고 식별 정보. 수동 등록 공고는 둘 다 null이며, NULL끼리는 unique 제약에 걸리지 않는다.
    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "external_id")
    private String externalId;

    // 외부 수집 데이터의 주요 필드를 해시로 압축한 값. 외부 소스를 재수집할 때 이 값만 비교해서
    // 변경 여부를 판단한다(PostingMapper.computeContentHash 참고). 수동 등록 공고는 null이다.
    @Column(name = "content_hash")
    private String contentHash;

    // 마감일이 지났거나(PostingMapper.resolveStatus) 외부 소스 목록에서 더 이상 조회되지 않으면(
    // PostingRepository.closeMissingFromSource) CLOSED로 전환된다. 물리 삭제는 하지 않는다.
    // columnDefinition에 DEFAULT를 명시해야 ddl-auto: update가 기존 행이 있는 테이블에도
    // NOT NULL 컬럼을 추가할 수 있다(DEFAULT 없이는 Postgres가 ALTER TABLE 자체를 거부한다).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'ACTIVE'")
    private PostingStatus status;

    @Builder
    private Posting(Code categoryCode, Code regionCode, String title, String company, String content,
                     LocalDate endDate, LocalDate startDate, String homepageUrl, String qualification,
                     String salary, String employmentType, String enterpriseType, String career, String education,
                     String sourceType, String externalId, String contentHash, PostingStatus status) {
        this.categoryCode = categoryCode;
        this.regionCode = regionCode;
        this.title = title;
        this.company = company;
        this.content = content;
        this.endDate = endDate;
        this.startDate = startDate;
        this.homepageUrl = homepageUrl;
        this.qualification = qualification;
        this.salary = salary;
        this.employmentType = employmentType;
        this.enterpriseType = enterpriseType;
        this.career = career;
        this.education = education;
        this.viewCount = 0L;
        this.scrapCount = 0L;
        this.sourceType = sourceType;
        this.externalId = externalId;
        this.contentHash = contentHash;
        this.status = status != null ? status : PostingStatus.ACTIVE;
    }

    // 외부 수집 공고의 변경된 필드를 갱신한다. 영속 상태의 엔티티에 호출해야 dirty checking으로 반영된다.
    public void updateFrom(Code categoryCode, Code regionCode, String title, String company, String content,
                            LocalDate endDate, LocalDate startDate, String homepageUrl, String qualification,
                            String salary, String employmentType, String enterpriseType, String career, String education,
                            String contentHash, PostingStatus status) {
        this.categoryCode = categoryCode;
        this.regionCode = regionCode;
        this.title = title;
        this.company = company;
        this.content = content;
        this.endDate = endDate;
        this.startDate = startDate;
        this.homepageUrl = homepageUrl;
        this.qualification = qualification;
        this.salary = salary;
        this.employmentType = employmentType;
        this.enterpriseType = enterpriseType;
        this.career = career;
        this.education = education;
        this.contentHash = contentHash;
        this.status = status;
    }

    // 응답 DTO(PostingListResponse 등)에서 사용하는 파생 값이라 별도 컬럼으로 저장하지 않는다.
    // 비로그인/관심사 없는 사용자를 위한 기본(비개인화) 판단. 마감 임박 여부만 본다.
    public boolean isRecommended() {
        return isRecommended(List.of());
    }

    // 로그인 사용자의 관심 카테고리 ID와 일치하면 우선 추천으로 간주하고, 그렇지 않으면 기존
    // 마감 임박 기준으로 판단한다(기존 사용자에게도 동일하게 fallback으로 적용됨).
    public boolean isRecommended(List<Long> interestCategoryIds) {
        if (status != PostingStatus.ACTIVE) {
            return false;
        }
        if (interestCategoryIds != null && categoryCode != null
                && interestCategoryIds.contains(categoryCode.getId())) {
            return true;
        }
        if (endDate == null) {
            return false;
        }
        long daysUntilDeadline = ChronoUnit.DAYS.between(LocalDate.now(), endDate);
        return daysUntilDeadline >= 0 && daysUntilDeadline <= RECOMMENDED_DEADLINE_WITHIN_DAYS;
    }
}