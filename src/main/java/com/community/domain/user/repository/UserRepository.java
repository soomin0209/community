package com.community.domain.user.repository;

import com.community.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, UserCustomRepository {
    boolean existsByLoginId(String loginId);

    boolean existsByNickname(String nickname);

    Optional<User> findByLoginIdAndDeletedAtIsNull(String loginId);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByIdAndDeletedAtIsNull(Long userId);

    List<User> findAllByDeletedAtIsNull();

    List<User> findAllByIdInAndDeletedAtIsNull(List<Long> ids);
}
