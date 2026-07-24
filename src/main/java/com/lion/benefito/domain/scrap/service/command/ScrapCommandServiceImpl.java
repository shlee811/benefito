package com.lion.benefito.domain.scrap.service.command;

import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.domain.posting.exception.PostingErrorCode;
import com.lion.benefito.domain.posting.exception.PostingException;
import com.lion.benefito.domain.posting.repository.PostingRepository;
import com.lion.benefito.domain.scrap.entity.Scrap;
import com.lion.benefito.domain.scrap.exception.ScrapErrorCode;
import com.lion.benefito.domain.scrap.exception.ScrapException;
import com.lion.benefito.domain.scrap.repository.ScrapRepository;
import com.lion.benefito.domain.user.entity.User;
import com.lion.benefito.domain.user.exception.UserErrorCode;
import com.lion.benefito.domain.user.exception.UserException;
import com.lion.benefito.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ScrapCommandServiceImpl implements ScrapCommandService {

    private final ScrapRepository scrapRepository;
    private final UserRepository userRepository;
    private final PostingRepository postingRepository;

    @Override
    public void addScrap(Long userId, Long postingId) {

        log.info("[ ScrapCommandService ] 공고 스크랩 추가를 시작합니다. userId={}, postingId={}", userId, postingId);

        // 1. 중복 스크랩 검증
        if (scrapRepository.existsByUserIdAndPostingId(userId, postingId)) {
            throw new ScrapException(ScrapErrorCode.SCRAP_ALREADY_EXISTS);
        }

        // 2. 정상 회원인지 조회 검증 (소프트 딜리트 탈퇴 유저 제외)
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        // 3. 대상 공고 검증 및 프록시 조회 (DB Vector 컬럼 매핑 에러 우회)
        if (!postingRepository.existsById(postingId)) {
            throw new PostingException(PostingErrorCode.POSTING_NOT_FOUND);
        }
        Posting posting = postingRepository.getReferenceById(postingId);

        // 4. 스크랩 생성 및 영속화
        Scrap scrap = Scrap.builder()
                .user(user)
                .posting(posting)
                .build();

        scrapRepository.save(scrap);
        postingRepository.incrementScrapCount(postingId);

        log.info("[ ScrapCommandService ] 공고 스크랩이 성공적으로 추가되었습니다. scrapId={}", scrap.getId());
    }

    @Override
    public void deleteScrap(Long userId, Long postingId) {

        log.info("[ ScrapCommandService ] 공고 스크랩 취소를 시작합니다. userId={}, postingId={}", userId, postingId);

        // 1. 정상 회원인지 조회 검증 (소프트 딜리트 탈퇴 유저 제외)
        userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        // 2. 대상 공고 검증 (DB Vector 컬럼 매핑 에러 우회)
        if (!postingRepository.existsById(postingId)) {
            throw new PostingException(PostingErrorCode.POSTING_NOT_FOUND);
        }

        // 3. 내 스크랩 정보 조회 및 삭제
        Scrap scrap = scrapRepository.findByUserIdAndPostingId(userId, postingId)
                .orElseThrow(() -> new ScrapException(ScrapErrorCode.SCRAP_NOT_FOUND));
        
        scrapRepository.delete(scrap);
        postingRepository.decrementScrapCount(postingId);

        log.info("[ ScrapCommandService ] 공고 스크랩이 성공적으로 취소되었습니다.");
    }
}
