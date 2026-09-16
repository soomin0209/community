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
    @JoinColumn(name = "suspended_by")
    private User suspendedBy;

    @Column(nullable = false, length = 50)
    private String reason;

    private Integer day;

    @Column(nullable = false)
    private boolean isPermanent = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime suspendedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unsuspended_by")
    private User unsuspendedBy;

    private LocalDateTime unsuspendedAt;

    public static UserSuspension temporarilySuspend(User user, User suspendedBy, String reason, int day, LocalDateTime now) {
        UserSuspension suspension = new UserSuspension();

        suspension.user = user;
        suspension.suspendedBy = suspendedBy;
        suspension.reason = reason;
        suspension.day = day;
        suspension.isPermanent = false;
        suspension.suspendedAt = now;

        return suspension;
    }

    public static UserSuspension permanentlySuspend(User user, User suspendedBy, String reason, LocalDateTime now) {
        UserSuspension suspension = new UserSuspension();

        suspension.user = user;
        suspension.suspendedBy = suspendedBy;
        suspension.reason = reason;
        suspension.day = null;
        suspension.isPermanent = true;
        suspension.suspendedAt = now;

        return suspension;
    }

    public void unsuspend(LocalDateTime now, User unsuspendedBy) {
        this.unsuspendedAt = now;
        this.unsuspendedBy = unsuspendedBy;
    }
}
