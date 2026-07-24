package com.lion.benefito.domain.posting.repository;

import java.time.LocalDate;

public interface PostingSimilarityProjection {

    Long getId();

    String getTitle();

    String getContent();

    String getCategoryCode();

    String getCompany();

    String getRegionCode();

    LocalDate getEndDate();

    String getSourceUrl();

    Double getDistance();
}
