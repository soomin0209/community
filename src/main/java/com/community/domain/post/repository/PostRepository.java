package com.community.domain.post.repository;

import com.community.domain.post.entity.Post;
import com.community.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, PostCustomRepository {
    Optional<Post> findByIdAndDeletedAtIsNull(Long id);

    User findByIdAndUserId(Long id, Long userId);
}
