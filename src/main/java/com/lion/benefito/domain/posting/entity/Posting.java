package com.lion.benefito.domain.posting.entity;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
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

    private String enterpriseType;

    private String employmentType;

    private String salary;

    @Column(columnDefinition = "TEXT")
    private String qualification;

    private String homepageUrl;

    @Column(nullable = false)
    private long viewCount;

    // title/content 기반 임베딩 벡터. PostingRepository.updateEmbedding() 벌크 쿼리로만 갱신한다(viewCount와 동일한 방식).
    @Column(columnDefinition = "vector(1536)")
    private String embedding;

    // 외부 수집 공고 식별 정보. 수동 등록 공고는 둘 다 null이며, NULL끼리는 unique 제약에 걸리지 않는다.
    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "external_id")
    private String externalId;

    @Builder
    private Posting(Code categoryCode, Code regionCode, String title, String company, String content,
                     LocalDate endDate, String homepageUrl, String qualification,
                     String sourceType, String externalId) {
        this.categoryCode = categoryCode;
        this.regionCode = regionCode;
        this.title = title;
        this.company = company;
        this.content = content;
        this.endDate = endDate;
        this.homepageUrl = homepageUrl;
        this.qualification = qualification;
        this.viewCount = 0L;
        this.sourceType = sourceType;
        this.externalId = externalId;
    }

    // 외부 수집 공고의 변경된 필드를 갱신한다. 영속 상태의 엔티티에 호출해야 dirty checking으로 반영된다.
    public void updateFrom(Code categoryCode, Code regionCode, String title, String company, String content,
                            LocalDate endDate, String homepageUrl, String qualification) {
        this.categoryCode = categoryCode;
        this.regionCode = regionCode;
        this.title = title;
        this.company = company;
        this.content = content;
        this.endDate = endDate;
        this.homepageUrl = homepageUrl;
        this.qualification = qualification;
    }
}