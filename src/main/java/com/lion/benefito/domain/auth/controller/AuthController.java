package com.lion.benefito.domain.auth.controller;

import com.lion.benefito.domain.user.dto.request.UserReqDTO;
import com.lion.benefito.domain.user.dto.response.UserResDTO;
import com.lion.benefito.domain.user.service.command.UserCommandService;
import com.lion.benefito.domain.user.service.query.UserQueryService;
import com.lion.benefito.global.common.CustomResponse;
import com.lion.benefito.global.security.jwt.JwtUtil;
import com.lion.benefito.global.security.jwt.dto.JwtDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Auth API", description = "인증 관련 API")
@RequestMapping("/auth")
public class AuthController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final JwtUtil jwtUtil; // JwtUtil 필드 주입 추가

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새로운 사용자로 가입합니다.")
    public CustomResponse<UserResDTO.SignUpResult> signUp(
            @RequestBody @Valid UserReqDTO.SignUp request) {

        log.info("[ AuthController ] 회원가입 요청 수신. loginId={}", request.loginId());

        UserResDTO.SignUpResult result = userCommandService.signUp(request);

        return CustomResponse.onSuccess(result);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "아이디와 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    public CustomResponse<JwtDTO> login(
            @RequestBody @Valid UserReqDTO.LoginReq request) {

        // 실제 로그인 처리는 Security의 CustomLoginFilter 필터가 요청을 낚아채어 수행합니다.
        // 이 컨트롤러 메서드는 오직 Swagger UI 상에 로그인 API 문서가 정상 노출되게 만들기 위한 껍데기(Dummy)입니다.
        return null;
    }

    @GetMapping("/check-id")
    @Operation(summary = "아이디 중복 확인", description = "입력한 로그인 ID의 중복 여부를 검사합니다. 중복인 경우 true, 사용 가능한 경우 false를 반환합니다.")
    public CustomResponse<Boolean> checkDuplicateLoginId(
            @RequestParam String loginId) {

        log.info("[ AuthController ] 아이디 중복 검증 요청 수신. loginId={}", loginId);

        boolean isDuplicate = userQueryService.checkDuplicateLoginId(loginId);

        return CustomResponse.onSuccess(isDuplicate);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 로그인된 회원을 로그아웃 처리하고 토큰을 블랙리스트에 등록합니다.")
    public CustomResponse<String> logout() {

        // 실제 로그아웃 처리는 Security의 CustomLogoutHandler 필터가 요청을 낚아채어 수행합니다.
        // 이 컨트롤러 메서드는 오직 Swagger UI 상에 로그아웃 API 문서가 정상 노출되게 만들기 위한 껍데기(Dummy)입니다.
        return null;
    }

    @PostMapping("/reissue")
    @Operation(summary = "토큰 재발급", description = "만료된 Access Token 대신 Refresh Token을 헤더(Authorization: Bearer <Refresh_Token>)에 실어 새 토큰 세트를 재발급받습니다.")
    public CustomResponse<JwtDTO> reissue(HttpServletRequest request) {

        log.info("[ AuthController ] 토큰 재발급 요청 수신.");

        String refreshToken = jwtUtil.resolveAccessToken(request);
        if (refreshToken == null) {
            throw new SecurityException("Refresh Token이 헤더에 존재하지 않습니다.");
        }

        JwtDTO tokenDto = jwtUtil.reissueToken(refreshToken);

        return CustomResponse.onSuccess(tokenDto);
    }
}
