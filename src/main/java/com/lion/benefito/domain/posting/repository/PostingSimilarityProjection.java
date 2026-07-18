package com.lion.benefito.domain.posting.repository;

public interface PostingSimilarityProjection {

    Long getId();

    String getTitle();

    String getContent();

    String getCategoryCode();

    String getSourceUrl();

    Double getDistance();
}
