package com.community.domain.user.enums;

import lombok.Getter;

@Getter
public enum UserGrade {
    BRONZE(1),
    SILVER(2),
    GOLD(3);

    private final int level;

    UserGrade(int level) {
        this.level = level;
    }
}
