package com.community.domain.user.entity;

import com.community.common.entity.BaseEntity;
import com.community.domain.user.enums.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static com.community.common.constant.AppConstants.*;

@Getter
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_deleted_at", columnList = "deletedAt")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String loginId;

    @Column(nullable = false, length = 16, unique = true)
    private String nickname;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private UserRole role = UserRole.BRONZE;

    @Column(nullable = false)
    private Long visitCount = 0L;

    @Column(nullable = false)
    private Long postCount = 0L;

    @Column(nullable = false)
    private Long commentCount = 0L;

    private LocalDateTime suspendedAt;
    private Long suspendedBy;
    private String suspendedReason;
    private int suspensionDay;

    public static User register(
            String loginId,
            String nickname,
            String password,
            UserRole role
    ) {
        User user = new User();

        user.loginId = loginId;
        user.nickname = nickname;
        user.password = password;
        user.role = role;

        return user;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void updateRoleByManager(UserRole role) {
        this.role = role;
    }

    public void increaseVisitCount() {
        this.visitCount += 1;
        updateRole();
    }

    public void increasePostCount() {
        this.postCount += 1;
        updateRole();
    }

    public void decreasePostCount() {
        if (this.postCount > 0) {
            this.postCount -= 1;
            updateRole();
        }
    }

    public void increaseCommentCount() {
        this.commentCount += 1;
        updateRole();
    }

    public void decreaseCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount -= 1;
            updateRole();
        }
    }

    public void setPostCount(Long postCount) {
        this.postCount = postCount;
    }

    public void setCommentCount(Long commentCount) {
        this.commentCount = commentCount;
    }

    public void updateRole() {
        if (this.role == UserRole.MANAGER || this.role == UserRole.ADMIN) {
            return;
        }
        if (this.visitCount >= GOLD_MIN_VISIT_COUNT &&
                this.postCount >= GOLD_MIN_POST_COUNT &&
                this.commentCount >= GOLD_MIN_COMMENT_COUNT) {
            this.role = UserRole.GOLD;
        } else if (this.visitCount >= SILVER_MIN_VISIT_COUNT &&
                this.postCount >= SILVER_MIN_POST_COUNT &&
                this.commentCount >= SILVER_MIN_COMMENT_COUNT) {
            this.role = UserRole.SILVER;
        } else {
            this.role = UserRole.BRONZE;
        }
    }

    public void suspendByManager(Long managerId, String suspendedReason, int suspensionDay) {
        this.suspendedAt = LocalDateTime.now();
        this.suspendedBy = managerId;
        this.suspendedReason = suspendedReason;
        this.suspensionDay = suspensionDay;
    }

    public boolean isSuspended() {
        if (this.getSuspendedAt() == null) {
            return false;
        }
        LocalDateTime suspendedUntil = this.getSuspendedAt().plusDays(this.getSuspensionDay());
        return LocalDateTime.now().isBefore(suspendedUntil);
    }

    public void unsuspend() {
        this.suspendedAt = null;
        this.suspendedBy = null;
        this.suspendedReason = null;
        this.suspensionDay = 0;
    }
}
