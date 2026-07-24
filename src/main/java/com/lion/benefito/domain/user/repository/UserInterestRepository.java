package com.lion.benefito.domain.user.repository;

import com.lion.benefito.domain.user.entity.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {

    List<UserInterest> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}