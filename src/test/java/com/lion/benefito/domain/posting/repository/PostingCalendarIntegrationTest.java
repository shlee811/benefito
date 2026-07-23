package com.lion.benefito.domain.posting.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.code.entity.CommonCodeType;
import com.lion.benefito.domain.code.repository.CodeRepository;
import com.lion.benefito.domain.posting.dto.PostingCalendarDayResponse;
import com.lion.benefito.domain.posting.dto.PostingCalendarDetailPageResponse;
import com.lion.benefito.domain.posting.dto.PostingCalendarItemResponse;
import com.lion.benefito.domain.posting.dto.PostingCalendarResponse;
import com.lion.benefito.domain.posting.entity.CalendarEventType;
import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.domain.posting.entity.PostingFilter;
import com.lion.benefito.domain.posting.entity.PostingStatus;
import com.lion.benefito.domain.posting.service.PostingService;
import com.lion.benefito.domain.user.entity.User;
import com.lion.benefito.domain.user.entity.UserInterest;
import com.lion.benefito.domain.user.repository.UserInterestRepository;
import com.lion.benefito.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

// 공고 캘린더(월별 상세 집계 + 날짜별 전체 목록)를 검증한다. 실제 운영 데이터(약 2870여건, 전부
// 2026년 근방 날짜)와 겹치지 않도록, 실존할 리 없는 미래 연월(2099-01)을 테스트 전용 슬롯으로
// 사용해 정확한 카운트 단언이 가능하게 한다(TITLE_PREFIX 키워드로는 격리할 수 없다 - 캘린더
// Specification은 keyword 조건이 없기 때문).
// @Transactional로 테스트 종료 후 자동 롤백되어 실제 데이터는 남지 않는다.
@SpringBootTest
@Transactional
class PostingCalendarIntegrationTest {

    private static final String TITLE_PREFIX = "캘린더테스트키워드";
    private static final int TEST_YEAR = 2099;
    private static final int TEST_MONTH = 1;

    @Autowired
    private PostingRepository postingRepository;

    @Autowired
    private CodeRepository codeRepository;

    @Autowired
    private PostingService postingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserInterestRepository userInterestRepository;

    @Autowired
    private EntityManager entityManager;

    private Code job() {
        return codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
    }

    private Code scholarship() {
        return codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "SCHOLARSHIP").orElseThrow();
    }

    private Code seoul() {
        return codeRepository.findByCodeTypeAndCode(CommonCodeType.REGION, "SEOUL").orElseThrow();
    }

    private Code busan() {
        return codeRepository.findByCodeTypeAndCode(CommonCodeType.REGION, "BUSAN").orElseThrow();
    }

    // createdAt(=임시 START 기준)은 @CreatedDate로 자동 관리되어 빌더로 지정할 수 없다. 저장 후
    // 네이티브 쿼리로 원하는 날짜로 강제 조정한다(PostingSortIntegrationTest의 forceCreatedAt과 동일한 기법).
    private Posting posting(String externalId, LocalDate createdAtDate, LocalDate endDate,
                             PostingStatus status, Code category, Code region) {
        Posting posting = Posting.builder()
                .categoryCode(category)
                .regionCode(region)
                .title(TITLE_PREFIX + " " + externalId)
                .company("테스트기관 " + externalId)
                .endDate(endDate)
                .status(status)
                .sourceType("TEST")
                .externalId(externalId)
                .build();
        Posting saved = postingRepository.save(posting);
        forceCreatedAt(saved.getId(), createdAtDate.atTime(12, 0));
        return saved;
    }

    private void forceCreatedAt(Long postingId, java.time.LocalDateTime createdAt) {
        entityManager.createNativeQuery("UPDATE postings SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", postingId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    private Optional<PostingCalendarDayResponse> findDay(PostingCalendarResponse response, LocalDate date) {
        return response.days().stream().filter(day -> day.date().equals(date)).findFirst();
    }

    @Test
    void 같은_날짜에_시작_공고가_여러_개면_startCount가_증가한다() {
        LocalDate day = LocalDate.of(TEST_YEAR, TEST_MONTH, 5);
        LocalDate farDeadline = LocalDate.of(TEST_YEAR, 6, 1);
        posting("start-multi-1", day, farDeadline, PostingStatus.ACTIVE, job(), null);
        posting("start-multi-2", day, farDeadline, PostingStatus.ACTIVE, job(), null);
        posting("start-multi-3", day, farDeadline, PostingStatus.ACTIVE, job(), null);

        PostingCalendarResponse response =
                postingService.getCalendar(TEST_YEAR, TEST_MONTH, null, null, PostingFilter.ALL, null, null);

        PostingCalendarDayResponse dayResponse = findDay(response, day).orElseThrow();
        assertThat(dayResponse.startCount()).isEqualTo(3);
        assertThat(dayResponse.deadlineCount()).isEqualTo(0);
        assertThat(dayResponse.totalCount()).isEqualTo(3);
    }

    @Test
    void 같은_날짜에_마감_공고가_여러_개면_deadlineCount가_증가한다() {
        LocalDate farCreatedAt = LocalDate.of(TEST_YEAR - 1, 12, 1);
        LocalDate deadline = LocalDate.of(TEST_YEAR, TEST_MONTH, 10);
        posting("deadline-multi-1", farCreatedAt, deadline, PostingStatus.ACTIVE, job(), null);
        posting("deadline-multi-2", farCreatedAt, deadline, PostingStatus.ACTIVE, job(), null);
        posting("deadline-multi-3", farCreatedAt, deadline, PostingStatus.ACTIVE, job(), null);

        PostingCalendarResponse response =
                postingService.getCalendar(TEST_YEAR, TEST_MONTH, null, null, PostingFilter.ALL, null, null);

        PostingCalendarDayResponse dayResponse = findDay(response, deadline).orElseThrow();
        assertThat(dayResponse.deadlineCount()).isEqualTo(3);
        assertThat(dayResponse.startCount()).isEqualTo(0);
        assertThat(dayResponse.totalCount()).isEqualTo(3);
    }

    @Test
    void 한_공고가_같은_날_시작이자_마감이면_START와_DEADLINE_둘_다_집계된다() {
        LocalDate sameDay = LocalDate.of(TEST_YEAR, TEST_MONTH, 15);
        Posting posting = posting("same-day-start-deadline", sameDay, sameDay, PostingStatus.ACTIVE, job(), null);

        PostingCalendarResponse response =
                postingService.getCalendar(TEST_YEAR, TEST_MONTH, null, null, PostingFilter.ALL, null, null);

        PostingCalendarDayResponse dayResponse = findDay(response, sameDay).orElseThrow();
        assertThat(dayResponse.startCount()).isEqualTo(1);
        assertThat(dayResponse.deadlineCount()).isEqualTo(1);
        assertThat(dayResponse.totalCount()).isEqualTo(2);

        List<CalendarEventType> types = dayResponse.items().stream()
                .filter(item -> item.postingId().equals(posting.getId()))
                .map(PostingCalendarItemResponse::type)
                .toList();
        assertThat(types).containsExactlyInAnyOrder(CalendarEventType.START, CalendarEventType.DEADLINE);
    }

    @Test
    void items는_최대_5개까지만_반환되고_초과분은_remainingCount로_계산된다() {
        LocalDate day = LocalDate.of(TEST_YEAR, TEST_MONTH, 20);
        LocalDate farDeadline = LocalDate.of(TEST_YEAR, 6, 1);
        for (int i = 1; i <= 7; i++) {
            posting("preview-cap-" + i, day, farDeadline, PostingStatus.ACTIVE, job(), null);
        }

        PostingCalendarResponse response =
                postingService.getCalendar(TEST_YEAR, TEST_MONTH, null, null, PostingFilter.ALL, null, null);

        PostingCalendarDayResponse dayResponse = findDay(response, day).orElseThrow();
        assertThat(dayResponse.startCount()).isEqualTo(7);
        assertThat(dayResponse.totalCount()).isEqualTo(7);
        assertThat(dayResponse.items()).hasSize(5);
        assertThat(dayResponse.remainingCount()).isEqualTo(2);
    }

    @Test
    void 다른_월의_공고는_집계에서_제외된다() {
        LocalDate prevMonth = LocalDate.of(TEST_YEAR - 1, 12, 31);
        LocalDate nextMonth = LocalDate.of(TEST_YEAR, TEST_MONTH + 1, 1);
        LocalDate farDeadline = LocalDate.of(TEST_YEAR, 6, 1);
        posting("other-month-prev", prevMonth, farDeadline, PostingStatus.ACTIVE, job(), null);
        posting("other-month-next", nextMonth, farDeadline, PostingStatus.ACTIVE, job(), null);

        PostingCalendarResponse response =
                postingService.getCalendar(TEST_YEAR, TEST_MONTH, null, null, PostingFilter.ALL, null, null);

        assertThat(findDay(response, prevMonth)).isEmpty();
        assertThat(findDay(response, nextMonth)).isEmpty();
    }

    @Test
    void category_필터가_적용된다() {
        LocalDate day = LocalDate.of(TEST_YEAR, TEST_MONTH, 6);
        LocalDate farDeadline = LocalDate.of(TEST_YEAR, 6, 1);
        posting("category-filter-job", day, farDeadline, PostingStatus.ACTIVE, job(), null);
        posting("category-filter-scholarship", day, farDeadline, PostingStatus.ACTIVE, scholarship(), null);

        PostingCalendarResponse response =
                postingService.getCalendar(TEST_YEAR, TEST_MONTH, "JOB", null, PostingFilter.ALL, null, null);

        PostingCalendarDayResponse dayResponse = findDay(response, day).orElseThrow();
        assertThat(dayResponse.startCount()).isEqualTo(1);
        assertThat(dayResponse.items()).extracting(PostingCalendarItemResponse::categoryCode)
                .containsExactly("JOB");
    }

    @Test
    void region_필터가_적용된다() {
        LocalDate day = LocalDate.of(TEST_YEAR, TEST_MONTH, 7);
        LocalDate farDeadline = LocalDate.of(TEST_YEAR, 6, 1);
        posting("region-filter-seoul", day, farDeadline, PostingStatus.ACTIVE, job(), seoul());
        posting("region-filter-busan", day, farDeadline, PostingStatus.ACTIVE, job(), busan());

        PostingCalendarResponse response =
                postingService.getCalendar(TEST_YEAR, TEST_MONTH, null, "SEOUL", PostingFilter.ALL, null, null);

        PostingCalendarDayResponse dayResponse = findDay(response, day).orElseThrow();
        assertThat(dayResponse.startCount()).isEqualTo(1);
        assertThat(dayResponse.items()).extracting(PostingCalendarItemResponse::region)
                .containsExactly("SEOUL");
    }

    @Test
    void category와_region이_ALL이거나_빈_문자열이면_필터가_적용되지_않는다() {
        LocalDate day = LocalDate.of(TEST_YEAR, TEST_MONTH, 8);
        LocalDate farDeadline = LocalDate.of(TEST_YEAR, 6, 1);
        posting("all-filter-job", day, farDeadline, PostingStatus.ACTIVE, job(), seoul());
        posting("all-filter-scholarship", day, farDeadline, PostingStatus.ACTIVE, scholarship(), busan());

        PostingCalendarResponse response =
                postingService.getCalendar(TEST_YEAR, TEST_MONTH, "ALL", "", PostingFilter.ALL, null, null);

        PostingCalendarDayResponse dayResponse = findDay(response, day).orElseThrow();
        assertThat(dayResponse.startCount()).isEqualTo(2);
    }

    @Test
    void 빈_월은_days가_빈_배열이다() {
        PostingCalendarResponse response =
                postingService.getCalendar(TEST_YEAR, 6, null, null, PostingFilter.ALL, null, null);

        assertThat(response.days()).isEmpty();
    }

    @Test
    void filter가_recommended면_관심_카테고리와_일치하는_공고만_캘린더에_포함된다() {
        LocalDate day = LocalDate.of(TEST_YEAR, TEST_MONTH, 9);
        LocalDate farDeadline = LocalDate.of(TEST_YEAR, 6, 1);
        posting("recommended-match-job", day, farDeadline, PostingStatus.ACTIVE, job(), null);
        posting("recommended-nomatch-scholarship", day, farDeadline, PostingStatus.ACTIVE, scholarship(), null);

        User user = userRepository.save(User.builder()
                .loginId("calendarRecUser").password("password").name("캘린더추천사용자").build());
        userInterestRepository.save(UserInterest.builder().user(user).categoryCode(job()).build());

        PostingCalendarResponse response = postingService.getCalendar(
                TEST_YEAR, TEST_MONTH, null, null, PostingFilter.RECOMMENDED, null, user.getId());

        PostingCalendarDayResponse dayResponse = findDay(response, day).orElseThrow();
        assertThat(dayResponse.startCount()).isEqualTo(1);
        assertThat(dayResponse.items()).extracting(PostingCalendarItemResponse::categoryCode)
                .containsExactly("JOB");
    }

    @Test
    void 날짜별_전체_목록은_5개_제한_없이_페이지네이션으로_전체를_반환한다() {
        LocalDate day = LocalDate.of(TEST_YEAR, TEST_MONTH, 25);
        LocalDate farDeadline = LocalDate.of(TEST_YEAR, 6, 1);
        for (int i = 1; i <= 7; i++) {
            posting("day-detail-" + i, day, farDeadline, PostingStatus.ACTIVE, job(), null);
        }

        PostingCalendarDetailPageResponse response = postingService.getCalendarDayDetail(
                day, null, null, null, PostingFilter.ALL, null, PageRequest.of(0, 20), null);

        assertThat(response.totalElements()).isEqualTo(7);
        assertThat(response.content()).hasSize(7);
    }

    @Test
    void type_파라미터로_START_DEADLINE을_구분해서_조회할_수_있다() {
        LocalDate day = LocalDate.of(TEST_YEAR, TEST_MONTH, 27);
        LocalDate farAwayDeadline = LocalDate.of(TEST_YEAR, 6, 1);
        LocalDate farAwayCreatedAt = LocalDate.of(TEST_YEAR - 1, 12, 1);
        posting("type-filter-start", day, farAwayDeadline, PostingStatus.ACTIVE, job(), null);
        posting("type-filter-deadline", farAwayCreatedAt, day, PostingStatus.ACTIVE, job(), null);

        PostingCalendarDetailPageResponse startOnly = postingService.getCalendarDayDetail(
                day, CalendarEventType.START, null, null, PostingFilter.ALL, null, PageRequest.of(0, 20), null);
        PostingCalendarDetailPageResponse deadlineOnly = postingService.getCalendarDayDetail(
                day, CalendarEventType.DEADLINE, null, null, PostingFilter.ALL, null, PageRequest.of(0, 20), null);

        assertThat(startOnly.content()).hasSize(1);
        assertThat(startOnly.content().get(0).type()).isEqualTo(CalendarEventType.START);
        assertThat(deadlineOnly.content()).hasSize(1);
        assertThat(deadlineOnly.content().get(0).type()).isEqualTo(CalendarEventType.DEADLINE);
    }

    @Test
    void status_파라미터를_지정하지_않으면_ACTIVE와_CLOSED_모두_포함되고_지정하면_해당_상태만_포함된다() {
        LocalDate day = LocalDate.of(TEST_YEAR, TEST_MONTH, 30);
        LocalDate farDeadline = LocalDate.of(TEST_YEAR, 6, 1);
        posting("status-active", day, farDeadline, PostingStatus.ACTIVE, job(), null);
        posting("status-closed", day, farDeadline, PostingStatus.CLOSED, job(), null);

        PostingCalendarResponse defaultResponse =
                postingService.getCalendar(TEST_YEAR, TEST_MONTH, null, null, PostingFilter.ALL, null, null);
        PostingCalendarResponse activeOnlyResponse = postingService.getCalendar(
                TEST_YEAR, TEST_MONTH, null, null, PostingFilter.ALL, PostingStatus.ACTIVE, null);

        assertThat(findDay(defaultResponse, day).orElseThrow().startCount()).isEqualTo(2);
        assertThat(findDay(activeOnlyResponse, day).orElseThrow().startCount()).isEqualTo(1);
    }
}
