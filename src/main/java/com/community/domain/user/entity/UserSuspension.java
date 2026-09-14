package com.community.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "user_suspensions", indexes = {
        @Index(name = "idx_user_suspension_user_id_unsuspended_at", columnList = "userId, unsuspendedAt")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSuspension {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @Column(nullable = false, length = 50)
    private String reason;

    private Integer day;

    @Column(nullable = false, updatable = false)
    private LocalDateTime suspendedAt;

    private LocalDateTime unsuspendedAt;

    public static UserSuspension suspend(User user, User manager, String reason, Integer day, LocalDateTime now) {
        UserSuspension suspension = new UserSuspension();

        suspension.user = user;
        suspension.manager = manager;
        suspension.reason = reason;
        suspension.day = day;
        suspension.suspendedAt = now;

        return suspension;
    }

    public void unsuspend(LocalDateTime now) {
        this.unsuspendedAt = now;
    }
}
