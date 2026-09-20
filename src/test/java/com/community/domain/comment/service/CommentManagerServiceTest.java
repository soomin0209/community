package com.community.domain.comment.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.comment.entity.Comment;
import com.community.domain.comment.exception.CommentExceptionEnum;
import com.community.domain.comment.repository.CommentRepository;
import com.community.domain.user.entity.User;
import com.community.domain.user.enums.UserRole;
import com.community.domain.user.repository.UserRepository;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CommentManagerServiceTest {

    @InjectMocks
    private CommentManagerService commentManagerService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;


    // ========== 댓글 강제 삭제 ==========
    @Test
    @DisplayName("댓글 강제 삭제 성공")
    void delete_success() {
        // given
        Long userId = 1L;

        User writer = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(writer, "id", 2L);

        Comment comment = Comment.register(null, 1L, writer.getId(), "맞아요! 산책하기 딱 좋은 날씨네요 ㅎㅎ", 0);
        ReflectionTestUtils.setField(comment, "id", 1L);

        given(commentRepository.findByIdAndDeletedAtIsNull(comment.getId())).willReturn(Optional.of(comment));
        given(userRepository.findByIdAndDeletedAtIsNull(comment.getUserId())).willReturn(Optional.of(writer));

        // when
        commentManagerService.delete(userId, comment.getId());

        // then
        assertThat(comment.getDeletedAt()).isNotNull();
        assertThat(comment.getDeletedBy()).isEqualTo(userId);
    }

    @Test
    @DisplayName("댓글 강제 삭제 성공 - 작성자 없음")
    void delete_success_writerNotFound() {
        // given
        Long userId = 1L;

        Comment comment = Comment.register(null, 1L, 2L, "맞아요! 산책하기 딱 좋은 날씨네요 ㅎㅎ", 0);
        ReflectionTestUtils.setField(comment, "id", 1L);

        given(commentRepository.findByIdAndDeletedAtIsNull(comment.getId())).willReturn(Optional.of(comment));
        given(userRepository.findByIdAndDeletedAtIsNull(comment.getUserId())).willReturn(Optional.empty());

        // when
        commentManagerService.delete(userId, comment.getId());

        // then
        assertThat(comment.getDeletedAt()).isNotNull();
        assertThat(comment.getDeletedBy()).isEqualTo(userId);
    }

    @Test
    @DisplayName("댓글 강제 삭제 실패 - 댓글 없음")
    void delete_fail_commentNotFound() {
        // given
        Long userId = 1L;
        Long commentId = 99L;

        given(commentRepository.findByIdAndDeletedAtIsNull(commentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentManagerService.delete(userId, commentId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CommentExceptionEnum.COMMENT_NOT_FOUND.getMessage());
    }
}