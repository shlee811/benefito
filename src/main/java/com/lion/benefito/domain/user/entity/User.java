package com.lion.benefito.domain.user.entity;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.user.enums.Grade;
import com.lion.benefito.domain.user.enums.Income;
import com.lion.benefito.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(name = "social_id")
    private String socialId;

    @Column
    private String school;

    @Column
    private String major;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    private Grade grade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_code_id")
    private Code regionCode;

    @Enumerated(EnumType.STRING)
    private Income income;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
