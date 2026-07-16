package com.lion.benefito.domain.posting.service;

import com.lion.benefito.domain.posting.exception.PostingErrorCode;
import com.lion.benefito.domain.posting.exception.PostingException;
import com.lion.benefito.domain.posting.dto.CalendarDeadlineCountResponse;
import com.lion.benefito.domain.posting.dto.PostingDetailResponse;
import com.lion.benefito.domain.posting.dto.PostingListResponse;
import com.lion.benefito.domain.posting.dto.PostingPageResponse;
import com.lion.benefito.domain.posting.dto.PostingSearchCondition;
import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.domain.posting.entity.PostingSort;
import com.lion.benefito.domain.posting.repository.PostingRepository;
import com.lion.benefito.domain.posting.repository.PostingSpecification;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostingService {

    private final PostingRepository postingRepository;

    public PostingPageResponse getPostings(PostingSearchCondition condition, Pageable pageable) {
        Page<Posting> postings = postingRepository.findAll(PostingSpecification.search(condition), pageable);
        return PostingPageResponse.from(postings.map(PostingListResponse::from));
    }

    public PostingDetailResponse getPosting(Long postingId) {
        Posting posting = postingRepository.findById(postingId)
                .orElseThrow(() -> new PostingException(PostingErrorCode.POSTING_NOT_FOUND));
        return PostingDetailResponse.from(posting);
    }

    public List<CalendarDeadlineCountResponse> getCalendar(YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.plusMonths(1).atDay(1);
        return postingRepository.countDeadlinesByEndDateBetween(startDate, endDate);
    }

    public List<PostingListResponse> getPostingsByDeadline(LocalDate date) {
        return postingRepository.findByEndDate(date, PostingSort.LATEST.toSort())
                .stream()
                .map(PostingListResponse::from)
                .toList();
    }

    @Transactional
    public void increaseViewCount(Long postingId) {
        int updatedCount = postingRepository.incrementViewCount(postingId);
        if (updatedCount == 0) {
            throw new PostingException(PostingErrorCode.POSTING_NOT_FOUND);
        }
    }
}
