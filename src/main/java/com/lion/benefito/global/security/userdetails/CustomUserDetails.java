package com.lion.benefito.global.security.userdetails;

import com.lion.benefito.domain.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    // 로그인한 유저의 도메인 객체가 필요할 때 꺼내 쓰는 Getter
    public User getUser() {
        return user;
    }

    // 해당 User의 권한을 return (로그인 회원인지 비로그인 회원인지 식별하기 위해)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        return authorities;
    }

    @Override
    public String getUsername() { return user.getLoginId(); }

    @Override
    public String getPassword() { return user.getPassword(); }

    // Account 가 만료되었는지?
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // Account 가 잠겨있는지?
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    // Credential 만료되지 않았는지?
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // 활성화가 되어있는지? (Soft Delete 여부 체크)
    @Override
    public boolean isEnabled() {
        // 탈퇴 날짜(deleted_at)가 null 이면 활성화 상태(true), 값이 있으면 탈퇴 상태(false)
        return true;
    }
}
