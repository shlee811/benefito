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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "postings")
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
}