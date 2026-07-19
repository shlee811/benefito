package com.lion.benefito.domain.scrap.service;

import com.lion.benefito.domain.scrap.entity.Scrap;
import com.lion.benefito.domain.scrap.exception.ScrapErrorCode;
import com.lion.benefito.domain.scrap.exception.ScrapException;
import com.lion.benefito.domain.scrap.repository.ScrapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScrapService {

    private final ScrapRepository scrapRepository;

    @Transactional
    public void deleteScrap(Long userId, Long postingId) {
        Scrap scrap = scrapRepository.findByUserIdAndPostingId(userId, postingId)
                .orElseThrow(() -> new ScrapException(ScrapErrorCode.SCRAP_NOT_FOUND));
        scrapRepository.delete(scrap);
    }
}
