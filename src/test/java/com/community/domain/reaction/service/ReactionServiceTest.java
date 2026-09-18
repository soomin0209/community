package com.community.domain.reaction.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.board.service.BoardService;
import com.community.domain.post.entity.Post;
import com.community.domain.post.enums.PostType;
import com.community.domain.post.exception.PostExceptionEnum;
import com.community.domain.post.repository.PostRepository;
import com.community.domain.reaction.dto.request.ReactionRequest;
import com.community.domain.reaction.dto.response.ReactionResponse;
import com.community.domain.reaction.entity.Reaction;
import com.community.domain.reaction.enums.ReactionType;
import com.community.domain.reaction.repository.ReactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReactionServiceTest {

    @InjectMocks
    private ReactionService reactionService;

    @Mock
    private ReactionRepository reactionRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private BoardService boardService;


    // ========== 좋아요/싫어요 ==========
    @Test
    @DisplayName("좋아요/싫어요 성공 - 좋아요")
    void react_success_like() {
        // given
        ReactionRequest request = new ReactionRequest(ReactionType.LIKE);
        Long userId = 1L;

        Post post = Post.register(userId, 1L, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        Reaction reaction = Reaction.register(post.getId(), userId, request.type());

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(reactionRepository.findByPostIdAndUserId(post.getId(), userId)).willReturn(Optional.empty());
        given(reactionRepository.save(any(Reaction.class))).willReturn(reaction);

        // when
        ReactionResponse response = reactionService.react(post.getId(), userId, request);

        // then
        assertThat(response.postId()).isEqualTo(post.getId());
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.type()).isEqualTo(request.type());
    }

    @Test
    @DisplayName("좋아요/싫어요 성공 - 좋아요 -> 싫어요 전환")
    void react_success_change() {
        // given
        ReactionRequest request = new ReactionRequest(ReactionType.DISLIKE);
        Long userId = 1L;

        Post post = Post.register(userId, 1L, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        Reaction likeReaction = Reaction.register(post.getId(), userId, ReactionType.LIKE);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(reactionRepository.findByPostIdAndUserId(post.getId(), userId)).willReturn(Optional.of(likeReaction));

        // when
        ReactionResponse response = reactionService.react(post.getId(), userId, request);

        // then
        assertThat(response.postId()).isEqualTo(post.getId());
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.type()).isEqualTo(request.type());
    }

    @Test
    @DisplayName("좋아요/싫어요 성공 - 좋아요 취소")
    void react_success_cancel() {
        // given
        ReactionRequest request = new ReactionRequest(ReactionType.LIKE);
        Long userId = 1L;

        Post post = Post.register(userId, 1L, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        Reaction likeReaction = Reaction.register(post.getId(), userId, ReactionType.LIKE);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(reactionRepository.findByPostIdAndUserId(post.getId(), userId)).willReturn(Optional.of(likeReaction));

        // when
        ReactionResponse response = reactionService.react(post.getId(), userId, request);

        // then
        assertThat(response).isNull();
        verify(reactionRepository).delete(likeReaction);
    }

    @Test
    @DisplayName("좋아요/싫어요 실패 - 게시물 없음")
    void react_fail_postNotFound() {
        // given
        ReactionRequest request = new ReactionRequest(ReactionType.LIKE);
        Long postId = 99L;

        given(postRepository.findByIdAndDeletedAtIsNull(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reactionService.react(postId, 1L, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(PostExceptionEnum.POST_NOT_FOUND.getMessage());
    }
}