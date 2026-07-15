package com.fail.app.domain.user.entity;

import com.fail.app.common.entity.SoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, unique = true, length = 30)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column
    private LocalDateTime lastLoginAt;

    @Builder
    private User(String email, String passwordHash, String nickname, UserRole role, UserStatus status) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.role = role;
        this.status = status;
    }

    public void restrict() {
        this.status = UserStatus.RESTRICTED;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }
}
