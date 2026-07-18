package com.lion.benefito.global.security.userdetails.service;

import com.lion.benefito.domain.user.entity.User;
import com.lion.benefito.domain.user.exception.UserErrorCode;
import com.lion.benefito.domain.user.exception.UserException;
import com.lion.benefito.domain.user.repository.UserRepository;
import com.lion.benefito.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Username(loginId) 으로 CustomUserDetail 가져오기
    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {

        log.info("[ CustomUserDetailsService ] LoginId 를 이용하여 User 를 검색합니다.");

        // 유저가 존재하면 CustomUserDetails 를 반환, 없으면 예외 처리
        Optional<User> userEntity = userRepository.findByLoginId(loginId);
        if(userEntity.isPresent()){
            User user = userEntity.get();
            return new CustomUserDetails(user);
        }
        throw new UserException(UserErrorCode.USER_NOT_FOUND);
    }
}
