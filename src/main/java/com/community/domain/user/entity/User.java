package com.community.domain.user.entity;

import com.community.common.entity.BaseEntity;
import com.community.domain.user.enums.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.community.common.constant.AppConstants.*;

@Getter
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_deleted_at", columnList = "deletedAt")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    @Column(nullable = false)
    private UserRole role = UserRole.BRONZE;

    @Column(nullable = false)
    private Long visitCount = 0L;

    @Column(nullable = false)
    private Long postCount = 0L;

    @Column(nullable = false)
    private Long commentCount = 0L;

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
        updateRole();
    }

    public void setCommentCount(Long commentCount) {
        this.commentCount = commentCount;
        updateRole();
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
}
