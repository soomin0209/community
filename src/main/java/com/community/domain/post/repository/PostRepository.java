package com.community.domain.post.repository;

import com.community.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, PostCustomRepository {
    Optional<Post> findByIdAndDeletedAtIsNull(Long id);

    Long countByUserIdAndDeletedAtIsNull(Long userId);

    Long countByDeletedAtIsNullAndIsPinnedTrue();
}
