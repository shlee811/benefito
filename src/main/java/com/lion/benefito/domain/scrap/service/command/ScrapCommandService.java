package com.lion.benefito.domain.scrap.service.command;

public interface ScrapCommandService {

    void addScrap(Long userId, Long postingId);

    void deleteScrap(Long userId, Long postingId);
}
