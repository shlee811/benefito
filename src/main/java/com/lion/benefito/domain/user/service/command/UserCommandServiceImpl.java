package com.lion.benefito.domain.user.service.command;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.code.entity.CommonCodeType;
import com.lion.benefito.domain.code.repository.CodeRepository;
import com.lion.benefito.domain.code.exception.CodeErrorCode;
import com.lion.benefito.domain.code.exception.CodeException;
import com.lion.benefito.domain.user.converter.UserConverter;
import com.lion.benefito.domain.user.dto.request.UserReqDTO;
import com.lion.benefito.domain.user.dto.response.UserResDTO;
import com.lion.benefito.domain.user.entity.User;
import com.lion.benefito.domain.user.entity.UserInterest;
import com.lion.benefito.domain.user.exception.UserErrorCode;
import com.lion.benefito.domain.user.exception.UserException;
import com.lion.benefito.domain.user.repository.UserRepository;
import com.lion.benefito.domain.user.repository.UserInterestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepository;
    private final UserInterestRepository userInterestRepository;
    private final CodeRepository codeRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public UserResDTO.SignUpResult signUp(UserReqDTO.SignUp request) {

        log.info("[ UserCommandService ] 회원가입을 시작합니다. loginId={}", request.loginId());

        // 1. 아이디 중복 검증 (소프트 딜리트 필터 적용)
        if (userRepository.existsByLoginIdAndDeletedAtIsNull(request.loginId())) {
            throw new UserException(UserErrorCode.USER_EMAIL_DUPLICATE);
        }

        // 2. 거주 지역 공통 코드 존재 여부 및 타입 검증 (영문 코드명 대조 적용)
        Code regionCode = codeRepository.findByCode(request.regionCode())
                .orElseThrow(() -> new CodeException(CodeErrorCode.CODE_NOT_FOUND));

        if (regionCode.getCodeType() != CommonCodeType.REGION) {
            throw new CodeException(CodeErrorCode.CODE_NOT_FOUND);
        }

        // 3. 비밀번호 암호화 및 유저 엔티티 영속화
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = UserConverter.toUser(request, encodedPassword, regionCode);
        User savedUser = userRepository.save(user);

        // 4. 관심 카테고리 매핑 정보(UserInterest) 생성 및 벌크 저장 (영문 코드명 대조 & 타입 검증 적용)
        if (request.categoryCodes() != null && !request.categoryCodes().isEmpty()) {
            List<UserInterest> interests = request.categoryCodes().stream()
                    .map(codeName -> {
                        Code categoryCode = codeRepository.findByCode(codeName)
                                .orElseThrow(() -> new CodeException(CodeErrorCode.CODE_NOT_FOUND));
                        if (categoryCode.getCodeType() != CommonCodeType.CATEGORY) {
                            throw new CodeException(CodeErrorCode.CODE_NOT_FOUND);
                        }
                        return UserInterest.builder()
                                .user(savedUser)
                                .categoryCode(categoryCode)
                                .build();
                    })
                    .toList();
            userInterestRepository.saveAll(interests);
        }

        log.info("[ UserCommandService ] 회원가입이 정상적으로 완료되었습니다. userId={}", savedUser.getId());

        return UserConverter.toSignUpResult(savedUser);
    }

    @Override
    public void softDeleteUser(Long userId) {

        log.info("[ UserCommandService ] 회원 탈퇴 처리를 시작합니다. userId={}", userId);

        // 탈퇴 대상 조회 (소프트 딜리트 필터 적용 -> 연속 탈퇴 시도 시 404 예외 발생)
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        // User 엔티티의 비즈니스 도메인 메서드를 통해 논리 삭제 시각 기록
        user.softDelete();
    }

    @Override
    public UserResDTO.MyProfile updateProfile(Long userId, UserReqDTO.UpdateProfile request) {

        log.info("[ UserCommandService ] 회원 프로필 수정을 시작합니다. userId={}", userId);

        // 1. 회원 조회 (소프트 딜리트 필터 적용)
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        // 2. 거주 지역 공통 코드 존재 여부 및 타입 검증 (영문 코드명 대조 적용)
        Code regionCode = codeRepository.findByCode(request.regionCode())
                .orElseThrow(() -> new CodeException(CodeErrorCode.CODE_NOT_FOUND));

        if (regionCode.getCodeType() != CommonCodeType.REGION) {
            throw new CodeException(CodeErrorCode.CODE_NOT_FOUND);
        }

        // 3. 유저 정보 갱신 (더티 체킹 자동 반영)
        user.updateProfile(
                request.name(),
                request.school(),
                request.major(),
                request.birthDate(),
                request.grade(),
                regionCode,
                request.income()
        );

        // 4. 기존 관심 분야 목록 로드 (프로필 수정에서 관심사 갱신 제외)
        List<UserInterest> userInterests = userInterestRepository.findByUserId(userId);
        List<String> categoryNames = userInterests.stream()
                .map(interest -> interest.getCategoryCode().getName())
                .toList();

        log.info("[ UserCommandService ] 회원 프로필 수정이 정상 완료되었습니다. loginId={}", user.getLoginId());

        return UserConverter.toMyProfile(user, categoryNames);
    }

    @Override
    public void updatePassword(Long userId, UserReqDTO.UpdatePassword request) {

        log.info("[ UserCommandService ] 회원 비밀번호 변경을 시작합니다. userId={}", userId);

        // 1. 회원 조회 (소프트 딜리트 필터 적용)
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        // 2. 현재 비밀번호 일치 검사
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new UserException(UserErrorCode.USER_WRONG_PASSWORD);
        }

        // 3. 동일한 비밀번호로 변경 방지 검사
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new UserException(UserErrorCode.PASSWORD_ALREADY_USED);
        }

        // 4. 신규 비밀번호 해싱 및 반영
        String encodedNewPassword = passwordEncoder.encode(request.newPassword());
        user.updatePassword(encodedNewPassword);

        log.info("[ UserCommandService ] 회원 비밀번호 변경이 완료되었습니다. loginId={}", user.getLoginId());
    }

    @Override
    public UserResDTO.UpdateInterestsResult updateInterests(Long userId, UserReqDTO.UpdateInterests request) {

        log.info("[ UserCommandService ] 회원 관심분야 수정을 시작합니다. userId={}", userId);

        // 1. 회원 조회 (소프트 딜리트 필터 적용)
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        // 2. 기존 관심 분야 매핑 정보 전체 삭제
        userInterestRepository.deleteByUserId(userId);

        // 3. 신규 관심 카테고리 매핑 생성 및 벌크 저장
        List<String> categoryNames = List.of();
        if (request.categoryCodes() != null && !request.categoryCodes().isEmpty()) {
            List<UserInterest> interests = request.categoryCodes().stream()
                    .map(codeName -> {
                        Code categoryCode = codeRepository.findByCode(codeName)
                                .orElseThrow(() -> new CodeException(CodeErrorCode.CODE_NOT_FOUND));
                        if (categoryCode.getCodeType() != CommonCodeType.CATEGORY) {
                            throw new CodeException(CodeErrorCode.CODE_NOT_FOUND);
                        }
                        return UserInterest.builder()
                                .user(user)
                                .categoryCode(categoryCode)
                                .build();
                    })
                    .toList();
            userInterestRepository.saveAll(interests);

            categoryNames = interests.stream()
                    .map(interest -> interest.getCategoryCode().getName())
                    .toList();
        }

        log.info("[ UserCommandService ] 회원 관심분야 수정이 정상 완료되었습니다. loginId={}", user.getLoginId());

        return UserResDTO.UpdateInterestsResult.builder()
                .categoryNames(categoryNames)
                .build();
    }
}
