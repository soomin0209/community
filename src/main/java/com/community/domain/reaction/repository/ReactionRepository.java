package com.community.domain.reaction.repository;

import com.community.domain.reaction.entity.Reaction;
import com.community.domain.reaction.enums.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    Optional<Reaction> findByPostIdAndUserId(Long postId, Long userId);

    Long countByPostIdAndType(Long id, ReactionType reactionType);
}
