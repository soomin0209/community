package com.community.domain.comment.service;

import com.community.common.dto.CursorResponse;
import com.community.common.dto.PageResponse;
import com.community.common.exception.ServiceErrorException;
import com.community.domain.board.service.BoardService;
import com.community.domain.comment.dto.request.CommentCursorCondition;
import com.community.domain.comment.dto.request.CommentPageCondition;
import com.community.domain.comment.dto.request.CreateCommentRequest;
import com.community.domain.comment.dto.request.UpdateCommentRequest;
import com.community.domain.comment.dto.response.CreateCommentResponse;
import com.community.domain.comment.dto.response.GetAllCommentsResponse;
import com.community.domain.comment.dto.response.GetMyCommentsResponse;
import com.community.domain.comment.dto.response.UpdateCommentResponse;
import com.community.domain.comment.entity.Comment;
import com.community.domain.comment.exception.CommentExceptionEnum;
import com.community.domain.comment.repository.CommentRepository;
import com.community.domain.post.entity.Post;
import com.community.domain.post.enums.PostType;
import com.community.domain.post.exception.PostExceptionEnum;
import com.community.domain.post.repository.PostRepository;
import com.community.domain.user.entity.User;
import com.community.domain.user.enums.UserRole;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import com.community.domain.user.service.UserRankingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRankingService userRankingService;

    @Mock
    private BoardService boardService;


    // ========== 댓글 등록 ==========
    @Test
    @DisplayName("댓글 등록 성공")
    void create_success() {
        // given
        CreateCommentRequest request = new CreateCommentRequest(null, "맞아요! 산책하기 딱 좋은 날씨네요 ㅎㅎ");

        Post post = Post.register(1L, 1L, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        User user = User.register("silver_user", "실버유저", "password", UserRole.SILVER);
        ReflectionTestUtils.setField(user, "id", 2L);

        Comment comment = Comment.register(request.parentId(), post.getId(), user.getId(), request.content(), 0);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(commentRepository.save(any(Comment.class))).willReturn(comment);

        // when
        CreateCommentResponse response = commentService.create(post.getId(), user.getId(), request);

        // then
        assertThat(response.parentId()).isEqualTo(request.parentId());
        assertThat(response.content()).isEqualTo(request.content());
    }

    @Test
    @DisplayName("댓글 등록 성공 - 대댓글")
    void create_success_childComment() {
        // given
        CreateCommentRequest request = new CreateCommentRequest(1L, "좋은 의견이네요! 감사합니다");

        Post post = Post.register(1L, 1L, "점심 메뉴 추천해주세요", "오늘 점심 뭐 먹을지 고민입니다. 추천 부탁드려요!", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 2L);

        User user = User.register("silver_user", "실버유저", "password", UserRole.SILVER);
        ReflectionTestUtils.setField(user, "id", 2L);

        Comment parentComment = Comment.register(null, 2L, 3L, "제육볶음 어떠세요?", 0);
        ReflectionTestUtils.setField(parentComment, "id", 1L);
        ReflectionTestUtils.setField(parentComment, "parentId", 1L);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(commentRepository.findByIdAndDeletedAtIsNull(request.parentId())).willReturn(Optional.of(parentComment));

        // when
        CreateCommentResponse response = commentService.create(post.getId(), user.getId(), request);

        // then
        assertThat(response.parentId()).isEqualTo(request.parentId());
        assertThat(response.content()).isEqualTo(request.content());
    }

    @Test
    @DisplayName("댓글 등록 실패 - 게시물 없음")
    void create_fail_postNotFound() {
        // given
        CreateCommentRequest request = new CreateCommentRequest(null, "맞아요! 산책하기 딱 좋은 날씨네요 ㅎㅎ");
        Long postId = 99L;
        Long userId = 1L;

        given(postRepository.findByIdAndDeletedAtIsNull(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.create(postId, userId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(PostExceptionEnum.POST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("댓글 등록 실패 - 사용자 없음")
    void create_fail_userNotFound() {
        // given
        CreateCommentRequest request = new CreateCommentRequest(null, "맞아요! 산책하기 딱 좋은 날씨네요 ㅎㅎ");

        Post post = Post.register(1L, 1L, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        Long userId = 99L;

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.create(post.getId(), userId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserExceptionEnum.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("댓글 등록 실패 - 부모 댓글 없음")
    void create_fail_parentCommentNotFound() {
        CreateCommentRequest request = new CreateCommentRequest(99L, "저도 나가볼까 봐요!");

        Post post = Post.register(1L, 1L, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        User user = User.register("silver_user", "실버유저", "password", UserRole.SILVER);
        ReflectionTestUtils.setField(user, "id", 2L);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(commentRepository.findByIdAndDeletedAtIsNull(request.parentId())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.create(post.getId(), user.getId(), request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CommentExceptionEnum.COMMENT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("댓글 등록 실패 - 부모 댓글의 게시물이 댓글 등록 게시물이 아님")
    void create_fail_commentInvalidParent() {
        // given
        CreateCommentRequest request = new CreateCommentRequest(1L, "좋은 의견이네요! 감사합니다");

        // 댓글을 달려는 게시물 id = 2
        // 부모 댓글의 게시물 id = 1
        Post post = Post.register(1L, 1L, "점심 메뉴 추천해주세요", "오늘 점심 뭐 먹을지 고민입니다. 추천 부탁드려요!", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 2L);

        User user = User.register("silver_user", "실버유저", "password", UserRole.SILVER);
        ReflectionTestUtils.setField(user, "id", 2L);

        Comment parentComment = Comment.register(null, 1L, 2L, "맞아요! 산책하기 딱 좋은 날씨네요 ㅎㅎ", 0);
        ReflectionTestUtils.setField(parentComment, "id", 1L);
        ReflectionTestUtils.setField(parentComment, "postId", 1L);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(commentRepository.findByIdAndDeletedAtIsNull(request.parentId())).willReturn(Optional.of(parentComment));

        // when & then
        assertThatThrownBy(() -> commentService.create(post.getId(), user.getId(), request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CommentExceptionEnum.COMMENT_INVALID_PARENT.getMessage());
    }

    @Test
    @DisplayName("댓글 등록 실패 - 댓글 깊이 초과")
    void create_fail_depthLimitExceed() {
        // given
        CreateCommentRequest request = new CreateCommentRequest(1L, "좋은 의견이네요! 감사합니다");

        Post post = Post.register(1L, 1L, "점심 메뉴 추천해주세요", "오늘 점심 뭐 먹을지 고민입니다. 추천 부탁드려요!", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 2L);

        User user = User.register("silver_user", "실버유저", "password", UserRole.SILVER);
        ReflectionTestUtils.setField(user, "id", 2L);

        Comment parentComment = Comment.register(null, 2L, 3L, "제육볶음 어떠세요?", 0);
        ReflectionTestUtils.setField(parentComment, "id", 1L);
        ReflectionTestUtils.setField(parentComment, "parentId", 1L);
        ReflectionTestUtils.setField(parentComment, "depth", 1);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(commentRepository.findByIdAndDeletedAtIsNull(request.parentId())).willReturn(Optional.of(parentComment));

        // when & then
        assertThatThrownBy(() -> commentService.create(post.getId(), user.getId(), request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CommentExceptionEnum.COMMENT_DEPTH_LIMIT_EXCEED.getMessage());
    }


    // ========== 댓글 목록 조회 ==========
    @Test
    @DisplayName("댓글 목록 조회 성공")
    void getAll_success() {
        // given
        Long postId = 1L;
        Long userId = 1L;

        CommentCursorCondition condition = new CommentCursorCondition();

        Post post = Post.register(2L, 1L, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", postId);

        GetAllCommentsResponse comment1 = new GetAllCommentsResponse(1L, null, "실버유저", false, "맞아요! 산책하기 딱 좋은 날씨네요 ㅎㅎ", LocalDateTime.now(), 0);
        GetAllCommentsResponse comment2 = new GetAllCommentsResponse(2L, null, "골드유저", true, "날씨 좋을 때 야외 활동 추천합니다", LocalDateTime.now(), 0);
        GetAllCommentsResponse comment3 = new GetAllCommentsResponse(3L, 1L, "브론즈유저", false, "저도 나가볼까 봐요!", LocalDateTime.now(), 1);

        given(postRepository.findByIdAndDeletedAtIsNull(postId)).willReturn(Optional.of(post));
        given(commentRepository.findParentCommentsWithCursor(condition.getCursor(), condition.getSize(), postId)).willReturn(List.of(comment1, comment2));
        given(commentRepository.findChildCommentsByParentIds(List.of(1L, 2L))).willReturn(List.of(comment3));

        // when
        CursorResponse<GetAllCommentsResponse> response = commentService.getAll(postId, condition, userId);

        // then
        assertThat(response.hasNext()).isFalse();
        assertThat(response.content()).hasSize(2);
        assertThat(response.content().getFirst().getChildren()).hasSize(1);
        assertThat(response.content().getFirst().getChildren().getFirst().getContent()).isEqualTo("저도 나가볼까 봐요!");
    }

    @Test
    @DisplayName("댓글 목록 조회 성공 - 댓글 없음")
    void getAll_success_empty() {
        // given
        Long userId = 1L;

        CommentCursorCondition condition = new CommentCursorCondition();

        Post post = Post.register(2L, 1L, "Spring Boot 학습 자료", "유용한 Spring Boot 학습 자료 모음입니다.", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(commentRepository.findParentCommentsWithCursor(condition.getCursor(), condition.getSize(), post.getId())).willReturn(List.of());

        // when
        CursorResponse<GetAllCommentsResponse> response = commentService.getAll(post.getId(), condition, userId);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("댓글 목록 조회 성공 - 게시물 작성자는 게시판 권한 검증 스킵")
    void getAll_success_postWriterBypass() {
        // given
        Long userId = 1L;

        CommentCursorCondition condition = new CommentCursorCondition();

        Post post = Post.register(userId, 1L, "Spring Boot 학습 자료", "유용한 Spring Boot 학습 자료 모음입니다.", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(commentRepository.findParentCommentsWithCursor(condition.getCursor(), condition.getSize(), post.getId())).willReturn(List.of());

        // when
        commentService.getAll(post.getId(), condition, userId);

        // then
        verify(boardService, never()).validateBoardAccess(any(), any());
    }

    @Test
    @DisplayName("댓글 목록 조회 성공 - 다음 페이지 존재")
    void getAll_success_hasNext() {
        // given
        Long userId = 1L;

        CommentCursorCondition condition = new CommentCursorCondition();
        condition.setSize(1);

        Post post = Post.register(2L, 1L, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        GetAllCommentsResponse comment1 = new GetAllCommentsResponse(1L, null, "실버유저", false, "맞아요! 산책하기 딱 좋은 날씨네요 ㅎㅎ", LocalDateTime.now(), 0);
        GetAllCommentsResponse comment2 = new GetAllCommentsResponse(2L, null, "골드유저", true, "날씨 좋을 때 야외 활동 추천합니다", LocalDateTime.now(), 0);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(commentRepository.findParentCommentsWithCursor(condition.getCursor(), condition.getSize(), post.getId())).willReturn(List.of(comment1, comment2));
        given(commentRepository.findChildCommentsByParentIds(List.of(1L))).willReturn(List.of());

        // when
        CursorResponse<GetAllCommentsResponse> response = commentService.getAll(post.getId(), condition, userId);

        // then
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(1L);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().getContent()).isEqualTo("맞아요! 산책하기 딱 좋은 날씨네요 ㅎㅎ");
    }

    @Test
    @DisplayName("댓글 목록 조회 실패 - 게시물 없음")
    void getAll_fail_postNotFound() {
        // given
        Long postId = 99L;
        Long userId = 1L;

        CommentCursorCondition condition = new CommentCursorCondition();

        given(postRepository.findByIdAndDeletedAtIsNull(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.getAll(postId, condition, userId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(PostExceptionEnum.POST_NOT_FOUND.getMessage());
    }


    // ========== 내 댓글 목록 조회 ==========
    @Test
    @DisplayName("내 댓글 목록 조회 성공")
    void getMine_success() {
        // given
        Long userId = 1L;

        CommentPageCondition condition = new CommentPageCondition();
        Pageable pageable = PageRequest.of(condition.getPage(), condition.getSize());

        GetMyCommentsResponse comment1 = new GetMyCommentsResponse(1L, 1L, "오늘 날씨 정말 좋네요!", "저도 나가볼까 봐요!", LocalDateTime.now());
        GetMyCommentsResponse comment2 = new GetMyCommentsResponse(2L, 2L, "점심 메뉴 추천해주세요", "좋은 의견이네요! 감사합니다", LocalDateTime.now());

        given(commentRepository.findMyCommentsWithCondition(pageable, userId)).willReturn(new PageImpl<>(List.of(comment1, comment2)));

        // when
        PageResponse<GetMyCommentsResponse> response = commentService.getMine(userId, condition);

        // then
        assertThat(response.content()).hasSize(2);
        assertThat(response.content().getFirst()).isEqualTo(comment1);
        assertThat(response.content().getLast()).isEqualTo(comment2);
    }


    // ========== 댓글 수정 ==========
    @Test
    @DisplayName("댓글 수정 성공")
    void update_success() {
        // given
        Long userId = 1L;

        UpdateCommentRequest request = new UpdateCommentRequest("산책하기 딱 좋은 날씨네요 ㅎㅎ");

        Post post = Post.register(2L, 1L, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        Comment comment = Comment.register(null, post.getId(), userId, "맞아요! 산책하기 딱 좋은 날씨네요 ㅎㅎ", 0);
        ReflectionTestUtils.setField(comment, "id", 1L);

        given(commentRepository.findByIdAndDeletedAtIsNull(comment.getId())).willReturn(Optional.of(comment));
        given(postRepository.findByIdAndDeletedAtIsNull(comment.getPostId())).willReturn(Optional.of(post));

        // when
        UpdateCommentResponse response = commentService.update(userId, comment.getId(), request);

        // then
        assertThat(response.content()).isEqualTo(request.content());
    }

    @Test
    @DisplayName("댓글 수정 실패 - 댓글 없음")
    void update_fail_commentNotFound() {
        // given
        UpdateCommentRequest request = new UpdateCommentRequest("산책하기 딱 좋은 날씨네요 ㅎㅎ");
        Long userId = 1L;
        Long commentId = 99L;

        given(commentRepository.findByIdAndDeletedAtIsNull(commentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.update(userId, commentId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CommentExceptionEnum.COMMENT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("댓글 수정 실패 - 작성자 아님")
    void update_fail_forbidden() {
        // given
        UpdateCommentRequest request = new UpdateCommentRequest("산책하기 딱 좋은 날씨네요 ㅎㅎ");
        Long userId = 1L;

        Comment comment = Comment.register(null, 1L, 2L, "맞아요! 산책하기 딱 좋은 날씨네요 ㅎㅎ", 0);
        ReflectionTestUtils.setField(comment, "id", 1L);

        given(commentRepository.findByIdAndDeletedAtIsNull(comment.getId())).willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> commentService.update(userId, comment.getId(), request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CommentExceptionEnum.COMMENT_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("댓글 수정 실패 - 게시물 없음")
    void update_fail_postNotFound() {
        // given
        UpdateCommentRequest request = new UpdateCommentRequest("산책하기 딱 좋은 날씨네요 ㅎㅎ");
        Long userId = 1L;
        Long postId = 99L;

        Comment comment = Comment.register(null, postId, userId, "맞아요! 산책하기 딱 좋은 날씨네요 ㅎㅎ", 0);
        ReflectionTestUtils.setField(comment, "id", 1L);

        given(commentRepository.findByIdAndDeletedAtIsNull(comment.getId())).willReturn(Optional.of(comment));
        given(postRepository.findByIdAndDeletedAtIsNull(comment.getPostId())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.update(userId, comment.getId(), request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(PostExceptionEnum.POST_NOT_FOUND.getMessage());
    }


    // ========== 댓글 삭제 ==========
    @Test
    @DisplayName("댓글 삭제 성공")
    void delete_success() {
        // given
        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        Comment comment = Comment.register(null, 1L, user.getId(), "맞아요! 산책하기 딱 좋은 날씨네요 ㅎㅎ", 0);
        ReflectionTestUtils.setField(comment, "id", 1L);

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(commentRepository.findByIdAndDeletedAtIsNull(comment.getId())).willReturn(Optional.of(comment));

        // when
        commentService.delete(user.getId(), comment.getId());

        // then
        assertThat(comment.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 사용자 없음")
    void delete_fail_userNotFound() {
        // given
        Long userId = 99L;
        Long commentId = 1L;

        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.delete(userId, commentId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserExceptionEnum.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 댓글 없음")
    void delete_fail_commentNotFound() {
        // given
        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        Long commentId = 99L;

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(commentRepository.findByIdAndDeletedAtIsNull(commentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.delete(user.getId(), commentId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CommentExceptionEnum.COMMENT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 작성자 아님")
    void delete_fail_forbidden() {
        // given
        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        Comment comment = Comment.register(null, 1L, 2L, "맞아요! 산책하기 딱 좋은 날씨네요 ㅎㅎ", 0);
        ReflectionTestUtils.setField(comment, "id", 1L);

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(commentRepository.findByIdAndDeletedAtIsNull(comment.getId())).willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> commentService.delete(user.getId(), comment.getId()))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CommentExceptionEnum.COMMENT_FORBIDDEN.getMessage());
    }
}