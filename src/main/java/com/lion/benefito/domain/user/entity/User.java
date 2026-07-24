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

    public void updateProfile(String name, String school, String major, LocalDate birthDate, Grade grade, Code regionCode, Income income) {
        this.name = name;
        this.school = school;
        this.major = major;
        this.birthDate = birthDate;
        this.grade = grade;
        this.regionCode = regionCode;
        this.income = income;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        // 탈퇴 시 로그인 ID 뒤에 UUID 난수를 덧붙여 물리적 UNIQUE 제약조건 충돌 우회
        this.loginId = this.loginId + "_withdrawn_" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}
