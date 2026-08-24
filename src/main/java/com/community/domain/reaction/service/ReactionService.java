package com.community.domain.reaction.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.post.entity.Post;
import com.community.domain.post.exception.PostExceptionEnum;
import com.community.domain.post.repository.PostRepository;
import com.community.domain.reaction.dto.request.ReactionRequest;
import com.community.domain.reaction.dto.response.ReactionResponse;
import com.community.domain.reaction.entity.Reaction;
import com.community.domain.reaction.repository.ReactionRepository;
import com.community.domain.user.entity.User;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public ReactionResponse react(Long postId, Long userId, ReactionRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        Post post = postRepository.findByIdAndDeletedAtIsNull(postId).orElseThrow(
                () -> new ServiceErrorException(PostExceptionEnum.POST_NOT_FOUND));

        Reaction reaction = reactionRepository.findByPostIdAndUserId(post.getId(), user.getId()).orElse(null);

        if (reaction != null) {
            if (reaction.getType() == request.type()) {
                reactionRepository.delete(reaction);
                return null;
            } else {
                reaction.update(request.type());
            }
        } else {
            reaction = Reaction.register(post.getId(), user.getId(), request.type());
            reactionRepository.save(reaction);
        }

        return new ReactionResponse(
                reaction.getId(),
                reaction.getPostId(),
                reaction.getUserId(),
                reaction.getType()
        );
    }
}
