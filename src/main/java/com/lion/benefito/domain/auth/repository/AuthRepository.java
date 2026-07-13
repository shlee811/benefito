package com.lion.benefito.domain.auth.repository;

import com.lion.benefito.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRepository extends JpaRepository<User, Long> {
}
