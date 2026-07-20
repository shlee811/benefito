package com.lion.benefito.domain.user.controller;

import com.lion.benefito.domain.user.dto.request.UserReqDTO;
import com.lion.benefito.domain.user.dto.response.UserResDTO;
import com.lion.benefito.domain.user.service.command.UserCommandService;
import com.lion.benefito.domain.user.service.query.UserQueryService;
import com.lion.benefito.global.common.CustomResponse;
import com.lion.benefito.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Tag(name = "User API", description = "유저 관련 API")
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    @GetMapping("/profile")
    @Operation(summary = "내 프로필 조회", description = "내 프로필을 조회합니다.")
    public CustomResponse<UserResDTO.MyProfile> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("[ UserController ] 내 프로필 조회 요청 수신. userId={}", userDetails.getUsername());

        UserResDTO.MyProfile myProfile = userQueryService.getMyProfile(userDetails.getUser().getId());

        return CustomResponse.onSuccess(myProfile);
    }

    @DeleteMapping("/me")
    @Operation(summary = "회원 탈퇴", description = "회원을 탈퇴합니다.")
    public CustomResponse<String> softDeleteUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("[ UserController ] 회원 탈퇴 요청 수신. userId={}", userDetails.getUsername());

        userCommandService.softDeleteUser(userDetails.getUser().getId());

        return CustomResponse.onSuccess("회원 탈퇴 성공");
    }

    @PatchMapping("/profile")
    @Operation(summary = "내 프로필 수정", description = "내 프로필 정보를 수정합니다.")
    public CustomResponse<UserResDTO.MyProfile> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserReqDTO.UpdateProfile request) {

        log.info("[ UserController ] 내 프로필 수정 요청 수신. userId={}", userDetails.getUsername());

        UserResDTO.MyProfile updatedProfile = userCommandService.updateProfile(
                userDetails.getUser().getId(), request);

        return CustomResponse.onSuccess(updatedProfile);
    }

    @PatchMapping("/password")
    @Operation(summary = "비밀번호 변경", description = "기존 비밀번호를 확인하고 새로운 비밀번호로 변경합니다.")
    public CustomResponse<String> updatePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserReqDTO.UpdatePassword request) {

        log.info("[ UserController ] 비밀번호 변경 요청 수신. userId={}", userDetails.getUsername());

        userCommandService.updatePassword(userDetails.getUser().getId(), request);

        return CustomResponse.onSuccess("비밀번호 변경 성공");
    }

    @PatchMapping("/interests")
    @Operation(summary = "관심 분야 수정", description = "사용자의 관심 분야를 수정합니다.")
    public CustomResponse<UserResDTO.UpdateInterestsResult> updateInterests(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserReqDTO.UpdateInterests request) {

        log.info("[ UserController ] 관심 분야 수정 요청 수신. userId={}", userDetails.getUsername());

        UserResDTO.UpdateInterestsResult result = userCommandService.updateInterests(
                userDetails.getUser().getId(), request);

        return CustomResponse.onSuccess(result);
    }
}
