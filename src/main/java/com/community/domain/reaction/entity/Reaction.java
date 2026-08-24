package com.community.domain.reaction.entity;

import com.community.domain.reaction.enums.ReactionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "reactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReactionType type;

    public static Reaction register(
            Long postId,
            Long userId,
            ReactionType type
    ) {
        Reaction reaction = new Reaction();

        reaction.postId = postId;
        reaction.userId = userId;
        reaction.type = type;

        return reaction;
    }
}
