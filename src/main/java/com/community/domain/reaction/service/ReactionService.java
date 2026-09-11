package com.community.domain.reaction.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.board.service.BoardService;
import com.community.domain.post.entity.Post;
import com.community.domain.post.exception.PostExceptionEnum;
import com.community.domain.post.repository.PostRepository;
import com.community.domain.reaction.dto.request.ReactionRequest;
import com.community.domain.reaction.dto.response.ReactionResponse;
import com.community.domain.reaction.entity.Reaction;
import com.community.domain.reaction.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final BoardService boardService;

    public ReactionResponse react(Long postId, Long userId, ReactionRequest request) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId).orElseThrow(
                () -> new ServiceErrorException(PostExceptionEnum.POST_NOT_FOUND));

        boardService.validateBoardAccess(userId, post.getBoardId());

        Reaction reaction = reactionRepository.findByPostIdAndUserId(post.getId(), userId).orElse(null);

        if (reaction != null) {
            if (reaction.getType() == request.type()) {
                reactionRepository.delete(reaction);
                return null;
            } else {
                reaction.update(request.type());
            }
        } else {
            reaction = Reaction.register(post.getId(), userId, request.type());
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
