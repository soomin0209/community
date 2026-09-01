package com.community.domain.user.repository;

import com.community.domain.user.dto.UserCountProjection;

import java.util.List;

public interface UserCustomRepository {
    List<UserCountProjection> findUserCounts();
}
