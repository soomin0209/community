package com.community.domain.user.repository;

import com.community.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByLoginId(String loginId);

    boolean existsByNickname(String nickname);

    Optional<User> findByLoginIdAndDeletedAtIsNull(String loginId);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);
}
