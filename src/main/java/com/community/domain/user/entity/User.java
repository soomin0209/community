package com.community.domain.user.entity;

import com.community.common.entity.BaseEntity;
import com.community.domain.user.enums.UserGrade;
import com.community.domain.user.enums.UserType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
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
    private UserType type;

    @Column(nullable = false)
    private Long visitCount = 0L;

    @Column(nullable = false)
    private Long postCount = 0L;

    @Column(nullable = false)
    private Long commentCount = 0L;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserGrade grade = UserGrade.BRONZE;

    public static User register(
            String loginId,
            String nickname,
            String password,
            UserType type
    ) {
        User user = new User();

        user.loginId = loginId;
        user.nickname = nickname;
        user.password = password;
        user.type = type;

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
        updateGrade();
    }

    public void increasePostCount() {
        this.postCount += 1;
        updateGrade();
    }

    public void decreasePostCount() {
        if (this.postCount > 0) {
            this.postCount -= 1;
            updateGrade();
        }
    }

    public void increaseCommentCount() {
        this.commentCount += 1;
        updateGrade();
    }

    public void decreaseCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount -= 1;
            updateGrade();
        }
    }

    public void setPostCount(Long postCount) {
        this.postCount = postCount;
        updateGrade();
    }

    public void setCommentCount(Long commentCount) {
        this.commentCount = commentCount;
        updateGrade();
    }

    public void updateGrade() {
        if (this.visitCount >= 30 && this.postCount >= 10 && this.commentCount >= 30) {
            this.grade = UserGrade.GOLD;
        } else if (this.visitCount >= 10 && this.postCount >= 3 && this.commentCount >= 10) {
            this.grade = UserGrade.SILVER;
        } else {
            this.grade = UserGrade.BRONZE;
        }
    }
}
