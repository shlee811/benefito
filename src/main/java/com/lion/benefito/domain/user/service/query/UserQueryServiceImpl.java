package com.lion.benefito.domain.user.service.query;

import com.lion.benefito.domain.user.converter.UserConverter;
import com.lion.benefito.domain.user.dto.response.UserResDTO;
import com.lion.benefito.domain.user.entity.User;
import com.lion.benefito.domain.user.entity.UserInterest;
import com.lion.benefito.domain.user.exception.UserErrorCode;
import com.lion.benefito.domain.user.exception.UserException;
import com.lion.benefito.domain.user.repository.UserInterestRepository;
import com.lion.benefito.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;
    private final UserInterestRepository userInterestRepository;

    @Override
    public UserResDTO.MyProfile getMyProfile(Long userId) {

        log.info("[ UserQueryService ] 마이 프로필 조회를 시작합니다. userId={}", userId);

        // 1. 회원 조회 (소프트 딜리트 필터 적용 -> 탈퇴 회원은 조회되지 않음)
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        // 2. 관심 카테고리 정보 조회 및 한글 이름 매핑
        List<UserInterest> interests = userInterestRepository.findByUserId(userId);
        List<String> categoryNames = interests.stream()
                .map(interest -> interest.getCategoryCode().getName())
                .toList();

        log.info("[ UserQueryService ] 마이 프로필 조회가 완료되었습니다. loginId={}", user.getLoginId());

        return UserConverter.toMyProfile(user, categoryNames);
    }

    @Override
    public boolean checkDuplicateLoginId(String loginId) {

        log.info("[ UserQueryService ] 아이디 중복 여부를 조회합니다. loginId={}", loginId);

        // 소프트 딜리트 필터 적용 -> 탈퇴한 계정의 ID는 중복 체크 결과 false 반환
        return userRepository.existsByLoginIdAndDeletedAtIsNull(loginId);
    }
}
