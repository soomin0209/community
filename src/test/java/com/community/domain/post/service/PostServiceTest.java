package com.community.domain.post.service;

import com.community.common.dto.PageResponse;
import com.community.common.exception.ServiceErrorException;
import com.community.domain.board.exception.BoardExceptionEnum;
import com.community.domain.board.repository.BoardRepository;
import com.community.domain.board.service.BoardService;
import com.community.domain.comment.entity.Comment;
import com.community.domain.comment.repository.CommentRepository;
import com.community.domain.file.entity.File;
import com.community.domain.file.repository.FileRepository;
import com.community.domain.file.service.FileService;
import com.community.domain.post.dto.request.CreatePostRequest;
import com.community.domain.post.dto.request.PostPageCondition;
import com.community.domain.post.dto.request.UpdatePostRequest;
import com.community.domain.post.dto.response.CreatePostResponse;
import com.community.domain.post.dto.response.GetAllPostsResponse;
import com.community.domain.post.dto.response.GetOnePostResponse;
import com.community.domain.post.dto.response.UpdatePostResponse;
import com.community.domain.post.entity.Post;
import com.community.domain.post.enums.PostType;
import com.community.domain.post.exception.PostExceptionEnum;
import com.community.domain.post.repository.PostRepository;
import com.community.domain.reaction.enums.ReactionType;
import com.community.domain.reaction.repository.ReactionRepository;
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

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @InjectMocks
    private PostService postService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostViewService postViewService;

    @Mock
    private ReactionRepository reactionRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRankingService userRankingService;

    @Mock
    private FileService fileService;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardService boardService;


    // ========== 게시물 등록 ==========
    @Test
    @DisplayName("게시물 등록 성공")
    void create_success() {
        // given
        Long boardId = 1L;

        CreatePostRequest request = new CreatePostRequest(boardId, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL, List.of());

        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        Post post = Post.register(user.getId(), boardId, request.title(), request.content(), request.type());

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(boardRepository.existsById(request.boardId())).willReturn(true);
        given(postRepository.save(any(Post.class))).willReturn(post);

        // when
        CreatePostResponse response = postService.create(user.getId(), request);

        // then
        assertThat(response.boardId()).isEqualTo(boardId);
        assertThat(response.title()).isEqualTo(request.title());
        assertThat(response.content()).isEqualTo(request.content());
        assertThat(response.type()).isEqualTo(request.type());
    }

    @Test
    @DisplayName("게시물 등록 성공 - 매니저가 NOTICE 등록")
    void create_success_notice() {
        // given
        Long boardId = 1L;

        CreatePostRequest request = new CreatePostRequest(boardId, "[공지] 9월 정기 점검 안내", "9월 15일 새벽 2시~4시 정기 점검이 예정되어 있습니다.", PostType.NOTICE, List.of());

        User user = User.register("manager", "매니저", "password", UserRole.MANAGER);
        ReflectionTestUtils.setField(user, "id", 1L);

        Post post = Post.register(user.getId(), boardId, request.title(), request.content(), request.type());

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(boardRepository.existsById(request.boardId())).willReturn(true);
        given(postRepository.save(any(Post.class))).willReturn(post);

        // when
        CreatePostResponse response = postService.create(user.getId(), request);

        // then
        assertThat(response.boardId()).isEqualTo(boardId);
        assertThat(response.title()).isEqualTo(request.title());
        assertThat(response.content()).isEqualTo(request.content());
        assertThat(response.type()).isEqualTo(request.type());
    }

    @Test
    @DisplayName("게시물 등록 실패 - 사용자 없음")
    void create_fail_userNotFound() {
        // given
        Long boardId = 1L;
        Long userId = 99L;

        CreatePostRequest request = new CreatePostRequest(boardId, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL, List.of());

        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.create(userId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserExceptionEnum.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시물 등록 실패 - 게시판 없음")
    void create_fail_boardNotFound() {
        // given
        Long boardId = 99L;

        CreatePostRequest request = new CreatePostRequest(boardId, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL, List.of());

        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(boardRepository.existsById(request.boardId())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> postService.create(user.getId(), request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BoardExceptionEnum.BOARD_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시물 등록 실패 - 매니저 이하 사용자가 NOTICE 등록")
    void create_fail_noticeForbidden() {
        // given
        Long boardId = 1L;

        CreatePostRequest request = new CreatePostRequest(boardId, "[공지] 9월 정기 점검 안내", "9월 15일 새벽 2시~4시 정기 점검이 예정되어 있습니다.", PostType.NOTICE, List.of());

        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(boardRepository.existsById(request.boardId())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> postService.create(user.getId(), request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(PostExceptionEnum.POST_NOTICE_FORBIDDEN.getMessage());
    }


    // ========== 게시물 단건 조회 ==========
    @Test
    @DisplayName("게시물 단건 조회 성공")
    void getOne_success() {
        // given
        User writer = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(writer, "id", 1L);

        Long boardId = 1L;
        Post post = Post.register(writer.getId(), boardId, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(userRepository.findById(post.getUserId())).willReturn(Optional.of(writer));
        given(commentRepository.countByPostIdAndDeletedAtIsNull(post.getId())).willReturn(5L);
        given(reactionRepository.countByPostIdAndType(post.getId(), ReactionType.LIKE)).willReturn(10L);
        given(reactionRepository.countByPostIdAndType(post.getId(), ReactionType.DISLIKE)).willReturn(3L);

        // when
        GetOnePostResponse response = postService.getOne(post.getId(), "clientId", null);

        // then
        assertThat(response.boardId()).isEqualTo(boardId);
        assertThat(response.title()).isEqualTo(post.getTitle());
        assertThat(response.content()).isEqualTo(post.getContent());
        assertThat(response.type()).isEqualTo(post.getType());
        assertThat(response.commentCount()).isEqualTo(5L);
        assertThat(response.likeCount()).isEqualTo(10L);
        assertThat(response.dislikeCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("게시물 단건 조회 성공 - 자기 자신의 게시물 (게시판 검증 스킵)")
    void getOne_success_ownPost() {
        // given
        User writer = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(writer, "id", 1L);

        Long boardId = 1L;
        Post post = Post.register(writer.getId(), boardId, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(userRepository.findById(post.getUserId())).willReturn(Optional.of(writer));
        given(commentRepository.countByPostIdAndDeletedAtIsNull(post.getId())).willReturn(5L);
        given(reactionRepository.countByPostIdAndType(post.getId(), ReactionType.LIKE)).willReturn(10L);
        given(reactionRepository.countByPostIdAndType(post.getId(), ReactionType.DISLIKE)).willReturn(3L);

        // when
        GetOnePostResponse response = postService.getOne(post.getId(), "clientId", writer.getId());

        // then
        assertThat(response.boardId()).isEqualTo(boardId);
        assertThat(response.title()).isEqualTo(post.getTitle());
        assertThat(response.content()).isEqualTo(post.getContent());
        assertThat(response.type()).isEqualTo(post.getType());
        assertThat(response.commentCount()).isEqualTo(5L);
        assertThat(response.likeCount()).isEqualTo(10L);
        assertThat(response.dislikeCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("게시물 단건 조회 실패 - 게시물 없음")
    void getOne_fail_postNotFound() {
        // given
        Long postId = 99L;

        given(postRepository.findByIdAndDeletedAtIsNull(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.getOne(postId, "clientId", null))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(PostExceptionEnum.POST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시물 단건 조회 실패 - 게시물 작성자 없음")
    void getOne_fail_writerNotFound() {
        // given
        Long boardId = 1L;
        Long writerId = 99L;
        Post post = Post.register(writerId, boardId, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);

        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(userRepository.findById(post.getUserId())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.getOne(post.getId(), "clientId", null))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserExceptionEnum.USER_NOT_FOUND.getMessage());
    }


    // ========== 게시물 목록 조회 ==========
    @Test
    @DisplayName("게시물 목록 조회 성공")
    void getAll_success() {
        // given
        PostPageCondition condition = new PostPageCondition();
        Pageable pageable = PageRequest.of(condition.getPage(), condition.getSize());

        GetAllPostsResponse post1 = new GetAllPostsResponse(
                1L, 1L, "오늘 날씨 정말 좋네요!", "브론즈유저", PostType.GENERAL,
                false, LocalDateTime.now(), 5L, 45L
        );
        GetAllPostsResponse post2 = new GetAllPostsResponse(
                2L, 1L, "점심 메뉴 추천해주세요", "실버유저", PostType.GENERAL,
                false, LocalDateTime.now(), 5L, 32L
        );

        given(postRepository.findPostsWithCondition(pageable, condition.getSortType(), condition.getKeyword(), condition.getSearchType(), null, null))
                .willReturn(new PageImpl<>(List.of(post1, post2)));

        // when
        PageResponse<GetAllPostsResponse> response = postService.getAll(condition);

        // then
        assertThat(response.content()).hasSize(2);
        assertThat(response.content().getFirst()).isEqualTo(post1);
        assertThat(response.content().getLast()).isEqualTo(post2);
    }


    // ========== 게시물 목록 조회 ==========
    @Test
    @DisplayName("내 게시물 목록 조회 성공")
    void getMine_success() {
        // given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 20);
        PostPageCondition condition = new PostPageCondition();

        GetAllPostsResponse post1 = new GetAllPostsResponse(
                1L, 1L, "오늘 날씨 정말 좋네요!", "브론즈유저", PostType.GENERAL,
                false, LocalDateTime.now(), 5L, 45L
        );
        GetAllPostsResponse post2 = new GetAllPostsResponse(
                2L, 1L, "점심 메뉴 추천해주세요", "브론즈유저", PostType.GENERAL,
                false, LocalDateTime.now(), 5L, 32L
        );

        given(postRepository.findPostsWithCondition(pageable, condition.getSortType(), condition.getKeyword(), condition.getSearchType(), userId, null))
                .willReturn(new PageImpl<>(List.of(post1, post2)));

        // when
        PageResponse<GetAllPostsResponse> response = postService.getMine(userId, condition);

        // then
        assertThat(response.content()).hasSize(2);
        assertThat(response.content().getFirst()).isEqualTo(post1);
        assertThat(response.content().getLast()).isEqualTo(post2);
    }


    // ========== 게시물 수정 ===========
    @Test
    @DisplayName("게시물 수정 성공")
    void update_success() {
        // given
        Long boardId = 1L;

        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        Post post = Post.register(user.getId(), boardId, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        UpdatePostRequest request = new UpdatePostRequest(null, "오늘 날씨 시원하네요!", null, null);

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));

        // when
        UpdatePostResponse response = postService.update(user.getId(), post.getId(), request);

        // then
        assertThat(response.title()).isEqualTo(request.title());
    }

    @Test
    @DisplayName("게시물 수정 성공 - 게시판 이동")
    void update_success_moveBoard() {
        // given
        Long boardId = 1L;

        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        Post post = Post.register(user.getId(), boardId, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        UpdatePostRequest request = new UpdatePostRequest(2L, null, null, null);

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(boardRepository.existsById(request.boardId())).willReturn(true);

        // when
        UpdatePostResponse response = postService.update(user.getId(), post.getId(), request);

        // then
        assertThat(response.boardId()).isEqualTo(request.boardId());
    }

    @Test
    @DisplayName("게시물 수정 성공 - 수정값 동일")
    void update_success_sameContent() {
        // given
        Long boardId = 1L;

        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        Post post = Post.register(user.getId(), boardId, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        UpdatePostRequest request = new UpdatePostRequest(boardId, "오늘 날씨 시원하네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", null);

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));

        // when
        UpdatePostResponse response = postService.update(user.getId(), post.getId(), request);

        // then
        assertThat(response.boardId()).isEqualTo(request.boardId());
        assertThat(response.title()).isEqualTo(request.title());
        assertThat(response.content()).isEqualTo(request.content());
    }

    @Test
    @DisplayName("게시물 수정 실패 - 사용자 없음")
    void update_fail_userNotFound() {
        // given
        Long userId = 99L;
        Long postId = 1L;

        UpdatePostRequest request = new UpdatePostRequest(null, "오늘 날씨 시원하네요!", null, null);

        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.update(userId, postId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserExceptionEnum.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시물 수정 실패 - 게시물 없음")
    void update_fail_postNotFound() {
        // given
        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        Long postId = 99L;

        UpdatePostRequest request = new UpdatePostRequest(null, "오늘 날씨 시원하네요!", null, null);

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(postRepository.findByIdAndDeletedAtIsNull(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.update(user.getId(), postId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(PostExceptionEnum.POST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시물 수정 실패 - 작성자 아님")
    void update_fail_forbidden() {
        // given
        Long boardId = 1L;

        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        Post post = Post.register(user.getId(), boardId, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);
        ReflectionTestUtils.setField(post, "userId", 2L);

        UpdatePostRequest request = new UpdatePostRequest(null, "오늘 날씨 시원하네요!", null, null);

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> postService.update(user.getId(), post.getId(), request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(PostExceptionEnum.POST_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("게시물 수정 실패 - 이동할 게시판 없음")
    void update_fail_boardNotFound() {
        // given
        Long boardId = 1L;

        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        Post post = Post.register(user.getId(), boardId, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "userId", 1L);

        UpdatePostRequest request = new UpdatePostRequest(2L, null, null, null);

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(boardRepository.existsById(request.boardId())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> postService.update(user.getId(), post.getId(), request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BoardExceptionEnum.BOARD_NOT_FOUND.getMessage());
    }


    // ========== 게시물 삭제 ==========
    @Test
    @DisplayName("게시물 삭제 성공")
    void delete_success() {
        // given
        Long boardId = 1L;

        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        Post post = Post.register(user.getId(), boardId, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));

        // when
        postService.delete(user.getId(), post.getId());

        // then
        assertThat(post.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("게시물 삭제 성공 - 댓글과 파일이 있는 경우")
    void delete_success_withCommentsAndFiles() {
        // given
        Long boardId = 1L;

        User postWriter = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(postWriter, "id", 1L);

        User commentWriter = User.register("silver_user", "실버유저", "password", UserRole.SILVER);
        ReflectionTestUtils.setField(commentWriter, "id", 2L);

        Post post = Post.register(postWriter.getId(), boardId, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        Comment comment = Comment.register(null, post.getId(), commentWriter.getId(), "맞아요! 산책하기 딱 좋은 날씨네요 ㅎㅎ", 0);
        ReflectionTestUtils.setField(comment, "id", 1L);

        File file = File.register(postWriter.getId(), "originalFilename", "storePath", 1000L, "plain/text");
        ReflectionTestUtils.setField(file, "id", 1L);

        given(userRepository.findByIdAndDeletedAtIsNull(postWriter.getId())).willReturn(Optional.of(postWriter));
        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(commentRepository.findByPostIdAndDeletedAtIsNull(post.getId())).willReturn(List.of(comment));
        given(userRepository.findAllByIdInAndDeletedAtIsNull(List.of(commentWriter.getId()))).willReturn(List.of(commentWriter));
        given(fileRepository.findByPostIdAndDeletedAtIsNull(post.getId())).willReturn(List.of(file));

        // when
        postService.delete(postWriter.getId(), post.getId());

        // then
        assertThat(post.getDeletedAt()).isNotNull();
        assertThat(comment.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("게시물 삭제 성공 - 댓글 작성자가 삭제된 경우")
    void delete_success_deletedCommentWriter() {
        // given
        Long boardId = 1L;

        User postWriter = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(postWriter, "id", 1L);

        Post post = Post.register(postWriter.getId(), boardId, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);

        Comment comment = Comment.register(null, post.getId(), 99L, "맞아요! 산책하기 딱 좋은 날씨네요 ㅎㅎ", 0);
        ReflectionTestUtils.setField(comment, "id", 1L);

        File file = File.register(postWriter.getId(), "originalFilename", "storePath", 1000L, "plain/text");
        ReflectionTestUtils.setField(file, "id", 1L);

        given(userRepository.findByIdAndDeletedAtIsNull(postWriter.getId())).willReturn(Optional.of(postWriter));
        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));
        given(commentRepository.findByPostIdAndDeletedAtIsNull(post.getId())).willReturn(List.of(comment));
        given(userRepository.findAllByIdInAndDeletedAtIsNull(List.of(99L))).willReturn(List.of());
        given(fileRepository.findByPostIdAndDeletedAtIsNull(post.getId())).willReturn(List.of(file));

        // when
        postService.delete(postWriter.getId(), post.getId());

        // then
        assertThat(post.getDeletedAt()).isNotNull();
        assertThat(comment.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("게시물 삭제 실패 - 사용자 없음")
    void delete_fail_userNotFound() {
        // given
        Long userId = 99L;

        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.delete(userId, 1L))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserExceptionEnum.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시물 삭제 실패 - 게시물 없음")
    void delete_fail_postNotFound() {
        // given
        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        Long postId = 99L;

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(postRepository.findByIdAndDeletedAtIsNull(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.delete(user.getId(), postId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(PostExceptionEnum.POST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시물 삭제 실패 - 작성자 아님")
    void delete_fail_forbidden() {
        // given
        Long boardId = 1L;

        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        Post post = Post.register(user.getId(), boardId, "오늘 날씨 정말 좋네요!", "가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);
        ReflectionTestUtils.setField(post, "userId", 2L);

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(postRepository.findByIdAndDeletedAtIsNull(post.getId())).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> postService.delete(user.getId(), post.getId()))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(PostExceptionEnum.POST_FORBIDDEN.getMessage());
    }
}