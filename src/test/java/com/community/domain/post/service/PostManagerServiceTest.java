package com.community.domain.post.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.board.entity.Board;
import com.community.domain.board.exception.BoardExceptionEnum;
import com.community.domain.board.repository.BoardRepository;
import com.community.domain.board.service.BoardService;
import com.community.domain.comment.entity.Comment;
import com.community.domain.comment.repository.CommentRepository;
import com.community.domain.file.entity.File;
import com.community.domain.file.repository.FileRepository;
import com.community.domain.file.service.FileManagerService;
import com.community.domain.post.dto.request.MovePostRequest;
import com.community.domain.post.dto.response.MovePostResponse;
import com.community.domain.post.dto.response.PinPostResponse;
import com.community.domain.post.entity.Post;
import com.community.domain.post.enums.PostType;
import com.community.domain.post.exception.PostExceptionEnum;
import com.community.domain.post.repository.PostRepository;
import com.community.domain.user.entity.User;
import com.community.domain.user.enums.UserRole;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static com.community.common.constant.AppConstants.POST_MAX_PINNED_COUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PostManagerServiceTest {

    @InjectMocks
    private PostManagerService postManagerService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileManagerService fileManagerService;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardService boardService;


    // ========== 게시물 고정/해제 ==========
    @Test
    @DisplayName("게시물 고정 성공")
    void pin_success() {
        // given
        User writer = User.register("manager", "매니저", "password", UserRole.MANAGER);
        ReflectionTestUtils.setField(writer, "id", 1L);

        Post post = Post.register(writer.getId(), 1L, "[공지] 9월 정기 점검 안내", "9월 15일 새벽 2시~4시 정기 점검이 예정되어 있습니다.", PostType.NOTICE);
        ReflectionTestUtils.setField(post, "id", 1L);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(userRepository.findByIdAndDeletedAtIsNull(post.getUserId())).willReturn(Optional.of(writer));
        given(postRepository.countByDeletedAtIsNullAndIsPinnedTrue()).willReturn(1L);

        // when
        PinPostResponse response = postManagerService.pin(post.getId());

        // then
        assertThat(response.title()).isEqualTo("[공지] 9월 정기 점검 안내");
        assertThat(response.type()).isEqualTo(PostType.NOTICE);
        assertThat(response.isPinned()).isTrue();
    }

    @Test
    @DisplayName("게시물 고정 해제 성공")
    void unpin_success() {
        // given
        User writer = User.register("manager", "매니저", "password", UserRole.MANAGER);
        ReflectionTestUtils.setField(writer, "id", 1L);

        Post post = Post.register(writer.getId(), 1L, "[공지] 9월 정기 점검 안내", "9월 15일 새벽 2시~4시 정기 점검이 예정되어 있습니다.", PostType.NOTICE);
        ReflectionTestUtils.setField(post, "id", 1L);
        ReflectionTestUtils.setField(post, "isPinned", true);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(userRepository.findByIdAndDeletedAtIsNull(post.getUserId())).willReturn(Optional.of(writer));

        // when
        PinPostResponse response = postManagerService.pin(post.getId());

        // then
        assertThat(response.title()).isEqualTo("[공지] 9월 정기 점검 안내");
        assertThat(response.type()).isEqualTo(PostType.NOTICE);
        assertThat(response.isPinned()).isFalse();
    }

    @Test
    @DisplayName("게시물 고정 실패 - 게시물 없음")
    void pin_fail_postNotFound() {
        // given
        Long postId = 99L;

        given(postRepository.findByIdAndDeletedAtIsNull(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postManagerService.pin(postId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(PostExceptionEnum.POST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시물 고정 실패 - 작성자 없음")
    void pin_fail_writerNotFound() {
        // given
        Long writerId = 99L;

        Post post = Post.register(writerId, 1L, "[공지] 9월 정기 점검 안내", "9월 15일 새벽 2시~4시 정기 점검이 예정되어 있습니다.", PostType.NOTICE);
        ReflectionTestUtils.setField(post, "id", 1L);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(userRepository.findByIdAndDeletedAtIsNull(post.getUserId())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postManagerService.pin(post.getId()))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserExceptionEnum.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시물 고정 실패 - 최대 고정 수 초과")
    void pin_fail_pinLimitExceeded() {
        // given
        User writer = User.register("manager", "매니저", "password", UserRole.MANAGER);
        ReflectionTestUtils.setField(writer, "id", 1L);

        Post post = Post.register(writer.getId(), 1L, "[공지] 9월 정기 점검 안내", "9월 15일 새벽 2시~4시 정기 점검이 예정되어 있습니다.", PostType.NOTICE);
        ReflectionTestUtils.setField(post, "id", 1L);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(userRepository.findByIdAndDeletedAtIsNull(post.getUserId())).willReturn(Optional.of(writer));
        given(postRepository.countByDeletedAtIsNullAndIsPinnedTrue()).willReturn(Long.valueOf(POST_MAX_PINNED_COUNT));

        // when & then
        assertThatThrownBy(() -> postManagerService.pin(post.getId()))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(PostExceptionEnum.POST_PIN_LIMIT_EXCEEDED.getMessage());
    }


    // ========== 게시물 강제 이동 ==========
    @Test
    @DisplayName("게시물 강제 이동 성공")
    void move_success() {
        // given
        MovePostRequest request = new MovePostRequest(2L);

        User writer = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(writer, "id", 1L);

        Post post = Post.register(writer.getId(), 1L, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        Board board = Board.register("자유게시판", UserRole.BRONZE);
        ReflectionTestUtils.setField(board, "id", 2L);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(boardRepository.findById(request.boardId())).willReturn(Optional.of(board));
        given(userRepository.findByIdAndDeletedAtIsNull(post.getUserId())).willReturn(Optional.of(writer));

        // when
        MovePostResponse response = postManagerService.move(post.getId(), request);

        // then
        assertThat(response.boardId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("게시물 이동 실패 - 게시물 없음")
    void move_fail_postNotFound() {
        // given
        MovePostRequest request = new MovePostRequest(2L);
        Long postId = 99L;

        given(postRepository.findByIdAndDeletedAtIsNull(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postManagerService.move(postId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(PostExceptionEnum.POST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시물 이동 실패 - 게시판 없음")
    void move_fail_boardNotFound() {
        // given
        MovePostRequest request = new MovePostRequest(99L);

        Post post = Post.register(1L, 1L, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(boardRepository.findById(request.boardId())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postManagerService.move(post.getId(), request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BoardExceptionEnum.BOARD_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시물 이동 실패 - 작성자 없음")
    void move_fail_writerNotFound() {
        // given
        MovePostRequest request = new MovePostRequest(2L);
        Long writerId = 99L;

        Post post = Post.register(writerId, 1L, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        Board board = Board.register("자유게시판", UserRole.BRONZE);
        ReflectionTestUtils.setField(board, "id", 2L);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(boardRepository.findById(request.boardId())).willReturn(Optional.of(board));
        given(userRepository.findByIdAndDeletedAtIsNull(post.getUserId())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postManagerService.move(post.getId(), request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserExceptionEnum.USER_NOT_FOUND.getMessage());
    }


    // ========== 게시물 강제 삭제 ==========
    @Test
    @DisplayName("게시물 강제 삭제 성공")
    void delete_success() {
        // given
        User writer = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(writer, "id", 1L);

        Post post = Post.register(writer.getId(), 1L, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(userRepository.findByIdAndDeletedAtIsNull(post.getUserId())).willReturn(Optional.of(writer));

        // when
        postManagerService.delete(2L, post.getId());

        // then
        assertThat(post.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("게시물 강제 삭제 성공 - 작성자 없음")
    void delete_success_writerIsNull() {
        // given
        Post post = Post.register(1L, 1L, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(userRepository.findByIdAndDeletedAtIsNull(post.getUserId())).willReturn(Optional.empty());

        // when
        postManagerService.delete(2L, post.getId());

        // then
        assertThat(post.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("게시물 강제 삭제 성공 - 댓글과 파일이 있는 경우")
    void delete_success_withCommentsAndFiles() {
        // given
        User postWriter = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(postWriter, "id", 1L);

        User commentWriter = User.register("silver_user", "실버유저", "password", UserRole.SILVER);
        ReflectionTestUtils.setField(commentWriter, "id", 2L);

        Post post = Post.register(postWriter.getId(), 1L, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        Comment comment = Comment.register(null, post.getId(), commentWriter.getId(), "맞아요! 산책하기 딱 좋은 날씨네요 ㅎㅎ", 0);
        ReflectionTestUtils.setField(comment, "id", 1L);

        File file = File.register(postWriter.getId(), "originalFilename", "storePath", 1000L, "plain/text");
        ReflectionTestUtils.setField(file, "id", 1L);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(userRepository.findByIdAndDeletedAtIsNull(post.getUserId())).willReturn(Optional.of(postWriter));
        given(commentRepository.findByPostIdAndDeletedAtIsNull(post.getId())).willReturn(List.of(comment));
        given(userRepository.findAllByIdInAndDeletedAtIsNull(List.of(commentWriter.getId()))).willReturn(List.of(commentWriter));
        given(fileRepository.findByPostIdAndDeletedAtIsNull(post.getId())).willReturn(List.of(file));

        // when
        postManagerService.delete(2L, post.getId());

        // then
        assertThat(post.getDeletedAt()).isNotNull();
        assertThat(comment.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("게시물 강제 삭제 성공 - 댓글 작성자가 삭제된 경우")
    void delete_success_deletedCommentWriter() {
        // given
        User postWriter = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(postWriter, "id", 1L);

        Post post = Post.register(postWriter.getId(), 1L, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        Comment comment = Comment.register(null, post.getId(), 99L, "맞아요! 산책하기 딱 좋은 날씨네요 ㅎㅎ", 0);
        ReflectionTestUtils.setField(comment, "id", 1L);

        File file = File.register(postWriter.getId(), "originalFilename", "storePath", 1000L, "plain/text");
        ReflectionTestUtils.setField(file, "id", 1L);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(userRepository.findByIdAndDeletedAtIsNull(post.getUserId())).willReturn(Optional.of(postWriter));
        given(commentRepository.findByPostIdAndDeletedAtIsNull(post.getId())).willReturn(List.of(comment));
        given(userRepository.findAllByIdInAndDeletedAtIsNull(List.of(99L))).willReturn(List.of());
        given(fileRepository.findByPostIdAndDeletedAtIsNull(post.getId())).willReturn(List.of(file));

        // when
        postManagerService.delete(2L, post.getId());

        // then
        assertThat(post.getDeletedAt()).isNotNull();
        assertThat(comment.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("게시물 강제 삭제 실패 - 게시물 없음")
    void delete_fail_postNotFound() {
        // given
        Long postId = 99L;

        given(postRepository.findByIdAndDeletedAtIsNull(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postManagerService.delete(2L, postId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(PostExceptionEnum.POST_NOT_FOUND.getMessage());
    }


}