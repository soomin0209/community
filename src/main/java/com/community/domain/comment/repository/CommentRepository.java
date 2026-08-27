package com.community.domain.comment.repository;

import com.community.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long>, CommentCustomRepository {
    Optional<Comment> findByIdAndDeletedAtIsNull(Long id);

    List<Comment> findByPostIdAndDeletedAtIsNull(Long postId);

    Long countByUserIdAndDeletedAtIsNull(Long userId);

    Long countByPostIdAndDeletedAtIsNull(Long postId);
}
