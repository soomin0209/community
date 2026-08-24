package com.community.domain.reaction.dto.request;

import com.community.domain.reaction.enums.ReactionType;
import jakarta.validation.constraints.NotNull;

public record ReactionRequest(
        @NotNull(message = "좋아요/싫어요 선택해주세요")
        ReactionType type
) {}
