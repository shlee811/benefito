package com.lion.benefito.domain.user.repository;

import com.lion.benefito.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    Optional<User> findByLoginIdAndDeletedAtIsNull(String loginId);

    boolean existsByLoginIdAndDeletedAtIsNull(String loginId);

    List<User> findByDeletedAtBefore(LocalDateTime deletedAtBefore);
}
