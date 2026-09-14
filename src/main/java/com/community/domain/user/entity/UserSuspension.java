package com.community.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "user_suspensions", indexes = {
        @Index(name = "idx_user_suspension_user_id_unsuspended_at", columnList = "deletedAt, unsuspendedAt")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
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

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime suspendedAt;

    private LocalDateTime unsuspendedAt;
}
