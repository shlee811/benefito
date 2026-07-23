package com.lion.benefito.domain.scrap.service.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.code.entity.CommonCodeType;
import com.lion.benefito.domain.code.repository.CodeRepository;
import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.domain.posting.entity.PostingStatus;
import com.lion.benefito.domain.posting.repository.PostingRepository;
import com.lion.benefito.domain.scrap.repository.ScrapRepository;
import com.lion.benefito.domain.user.entity.User;
import com.lion.benefito.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

// sort=popular의 기준인 Posting.scrapCount가 실제 scraps 테이블 집계(ScrapRepository.countByPostingId)와
// 항상 일치하는지 검증한다(비정규화 컬럼이 실제 스크랩 데이터를 정확히 반영해야 함).
// @Transactional로 테스트 종료 후 자동 롤백되어 실제 데이터는 남지 않는다.
@SpringBootTest
@Transactional
class ScrapCommandServiceIntegrationTest {

    private static final String TITLE_PREFIX = "스크랩카운트테스트키워드";

    @Autowired
    private ScrapCommandService scrapCommandService;

    @Autowired
    private ScrapRepository scrapRepository;

    @Autowired
    private PostingRepository postingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CodeRepository codeRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 스크랩을_추가하면_Posting_scrapCount가_실제_scraps_테이블_집계와_일치한다() {
        Code category = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        Posting posting = postingRepository.save(Posting.builder()
                .categoryCode(category)
                .title(TITLE_PREFIX + " scrap-count-add")
                .company("테스트 회사")
                .endDate(LocalDate.now().plusDays(10))
                .status(PostingStatus.ACTIVE)
                .sourceType("TEST")
                .externalId("scrap-count-add")
                .build());
        User userA = userRepository.save(User.builder()
                .loginId("scrapCountUserA").password("password").name("스크랩카운트A").build());
        User userB = userRepository.save(User.builder()
                .loginId("scrapCountUserB").password("password").name("스크랩카운트B").build());

        scrapCommandService.addScrap(userA.getId(), posting.getId());
        scrapCommandService.addScrap(userB.getId(), posting.getId());
        entityManager.flush();
        entityManager.clear();

        long realCount = scrapRepository.countByPostingId(posting.getId());
        Posting reloaded = postingRepository.findById(posting.getId()).orElseThrow();
        assertThat(realCount).isEqualTo(2);
        assertThat(reloaded.getScrapCount()).isEqualTo(realCount);
    }

    @Test
    void 스크랩을_취소하면_Posting_scrapCount가_실제_scraps_테이블_집계와_일치한다() {
        Code category = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        Posting posting = postingRepository.save(Posting.builder()
                .categoryCode(category)
                .title(TITLE_PREFIX + " scrap-count-remove")
                .company("테스트 회사")
                .endDate(LocalDate.now().plusDays(10))
                .status(PostingStatus.ACTIVE)
                .sourceType("TEST")
                .externalId("scrap-count-remove")
                .build());
        User userA = userRepository.save(User.builder()
                .loginId("scrapCountUserC").password("password").name("스크랩카운트C").build());
        User userB = userRepository.save(User.builder()
                .loginId("scrapCountUserD").password("password").name("스크랩카운트D").build());

        scrapCommandService.addScrap(userA.getId(), posting.getId());
        scrapCommandService.addScrap(userB.getId(), posting.getId());
        scrapCommandService.deleteScrap(userA.getId(), posting.getId());
        entityManager.flush();
        entityManager.clear();

        long realCount = scrapRepository.countByPostingId(posting.getId());
        Posting reloaded = postingRepository.findById(posting.getId()).orElseThrow();
        assertThat(realCount).isEqualTo(1);
        assertThat(reloaded.getScrapCount()).isEqualTo(realCount);
    }
}
