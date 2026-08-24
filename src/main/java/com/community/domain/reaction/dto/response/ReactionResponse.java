package com.community.domain.reaction.dto.response;

import com.community.domain.reaction.enums.ReactionType;

public record ReactionResponse(
        Long id,
        Long postId,
        Long userId,
        ReactionType type
) {}
