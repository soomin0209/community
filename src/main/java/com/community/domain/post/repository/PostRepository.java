package com.community.domain.post.repository;

import com.community.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findByIdAndDeletedAtIsNull(Long id);
}
