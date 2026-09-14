package com.community.domain.user.repository;

import com.community.domain.user.entity.UserSuspension;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSuspensionRepository extends JpaRepository<UserSuspension, Long> {
}
