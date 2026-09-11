package com.community.domain.user.enums;

import lombok.Getter;

@Getter
public enum UserRole {
    BRONZE(1),
    SILVER(2),
    GOLD(3),
    MANAGER(4),
    ADMIN(5);

    private final int level;

    UserRole(int level) {
        this.level = level;
    }
}
