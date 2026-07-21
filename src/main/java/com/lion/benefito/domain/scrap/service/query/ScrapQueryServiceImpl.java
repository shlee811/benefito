package com.lion.benefito.domain.scrap.service.query;

import com.lion.benefito.domain.code.entity.CommonCodeType;
import com.lion.benefito.domain.code.repository.CodeRepository;
import com.lion.benefito.domain.scrap.dto.response.ScrapResDTO;
import com.lion.benefito.domain.scrap.entity.Scrap;
import com.lion.benefito.domain.scrap.exception.ScrapErrorCode;
import com.lion.benefito.domain.scrap.exception.ScrapException;
import com.lion.benefito.domain.scrap.repository.ScrapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScrapQueryServiceImpl implements ScrapQueryService {

    private final ScrapRepository scrapRepository;
    private final CodeRepository codeRepository;

    private void validateCategoryCode(String categoryCode) {
        if (categoryCode != null && !categoryCode.isBlank()) {
            codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, categoryCode)
                    .orElseThrow(() -> new ScrapException(ScrapErrorCode.INVALID_CATEGORY));
        }
    }

    @Override
    public ScrapResDTO.MyScrapList getMyScrapList(Long userId, String categoryCode) {

        log.info("[ ScrapQueryService ] 스크랩 목록 조회를 시작합니다. userId={}, categoryCode={}", userId, categoryCode);
        validateCategoryCode(categoryCode);

        // 1. 레포지토리를 통해 조건에 맞는 스크랩 목록 페치 조인 조회
        // 전체 조회의 경우 categoryCode 파라미터가 null로 들어오며, 쿼리 내부 로직에 의해 전체가 반환됩니다.
        List<Scrap> scraps = scrapRepository.findMyScrapsWithCategoryFilter(userId, categoryCode);

        // 2. 조회된 엔티티 리스트를 DTO 래퍼로 변환하여 반환
        List<ScrapResDTO.MyScrap> myScraps = scraps.stream()
                .map(scrap -> ScrapResDTO.MyScrap.builder()
                        .scrapId(scrap.getId())
                        .postingId(scrap.getPosting().getId())
                        .title(scrap.getPosting().getTitle())
                        .company(scrap.getPosting().getCompany())
                        .endDate(scrap.getPosting().getEndDate())
                        .categoryName(scrap.getPosting().getCategoryCode().getName())
                        .build())
                .toList();

        ScrapResDTO.MyScrapList myScrapList = ScrapResDTO.MyScrapList.builder()
                .scraps(myScraps)
                .build();

        log.info("[ ScrapQueryService ] 스크랩 목록 조회가 완료되었습니다.");

        return myScrapList;
    }

    @Override
    public ScrapResDTO.MyScrapList getWeeklyDeadlineScrapList(Long userId, String categoryCode) {

        log.info("[ ScrapQueryService ] 이번 주 마감 스크랩 목록 조회를 시작합니다. userId={}, categoryCode={}", userId, categoryCode);
        validateCategoryCode(categoryCode);

        java.time.LocalDate startDate = java.time.LocalDate.now();
        java.time.LocalDate endDate = startDate.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));

        List<Scrap> scraps = scrapRepository.findMyScrapsByWeeklyDeadline(userId, startDate, endDate, categoryCode);

        List<ScrapResDTO.MyScrap> myScraps = scraps.stream()
                .map(scrap -> {
                    java.time.LocalDate deadline = scrap.getPosting().getEndDate();
                    Long dDay = (deadline != null) ? java.time.temporal.ChronoUnit.DAYS.between(startDate, deadline) : null;
                    return ScrapResDTO.MyScrap.builder()
                            .scrapId(scrap.getId())
                            .postingId(scrap.getPosting().getId())
                            .title(scrap.getPosting().getTitle())
                            .company(scrap.getPosting().getCompany())
                            .endDate(deadline)
                            .categoryName(scrap.getPosting().getCategoryCode().getName())
                            .dDay(dDay)
                            .build();
                })
                .toList();

        ScrapResDTO.MyScrapList myScrapList = ScrapResDTO.MyScrapList.builder()
                .scraps(myScraps)
                .build();

        log.info("[ ScrapQueryService ] 이번 주 마감 스크랩 목록 조회가 완료되었습니다. 건수={}", scraps.size());

        return myScrapList;
    }

    @Override
    public ScrapResDTO.ScrapSummary getScrapSummary(Long userId) {

        log.info("[ ScrapQueryService ] 스크랩 요약 통계 조회를 시작합니다. userId={}", userId);

        long totalCount = scrapRepository.countByUserId(userId);

        java.time.LocalDate startDate = java.time.LocalDate.now();
        java.time.LocalDate endDate = startDate.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
        long weeklyDeadlineCount = scrapRepository.countByUserIdAndPostingEndDateBetween(userId, startDate, endDate);

        ScrapResDTO.ScrapSummary summary = ScrapResDTO.ScrapSummary.builder()
                .totalCount(totalCount)
                .weeklyDeadlineCount(weeklyDeadlineCount)
                .build();

        log.info("[ ScrapQueryService ] 스크랩 요약 통계 조회가 완료되었습니다. 전체={}, 이번주마감={}", totalCount, weeklyDeadlineCount);

        return summary;
    }

    @Override
    public ScrapResDTO.PostingScrapCount getScrapCountByPosting(Long postingId) {

        log.info("[ ScrapQueryService ] 특정 공고 스크랩 수 조회를 시작합니다. postingId={}", postingId);

        long scrapCount = scrapRepository.countByPostingId(postingId);

        ScrapResDTO.PostingScrapCount response = ScrapResDTO.PostingScrapCount.builder()
                .postingId(postingId)
                .scrapCount(scrapCount)
                .build();

        log.info("[ ScrapQueryService ] 특정 공고 스크랩 수 조회가 완료되었습니다. postingId={}, scrapCount={}", postingId, scrapCount);

        return response;
    }
}
