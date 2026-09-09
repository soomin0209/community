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
import com.community.domain.post.exception.PostExceptionEnum;
import com.community.domain.post.repository.PostRepository;
import com.community.domain.user.entity.User;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.community.common.constant.AppConstants.POST_MAX_PINNED_COUNT;

@Service
@RequiredArgsConstructor
@Transactional
public class PostManagerService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final FileRepository fileRepository;
    private final FileManagerService fileManagerService;
    private final BoardRepository boardRepository;
    private final BoardService boardService;

    // 게시물 고정/해제
    public PinPostResponse pin(Long userId, Long postId) {
        if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
            throw new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND);
        }

        Post post = postRepository.findByIdAndDeletedAtIsNull(postId).orElseThrow(
                () -> new ServiceErrorException(PostExceptionEnum.POST_NOT_FOUND));

        User writer = userRepository.findByIdAndDeletedAtIsNull(post.getUserId()).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        if (post.getIsPinned()) {
            post.unpin();
        } else {
            Long pinnedCount = postRepository.countByDeletedAtIsNullAndIsPinnedTrue();
            if (pinnedCount >= POST_MAX_PINNED_COUNT) {
                throw new ServiceErrorException(PostExceptionEnum.POST_PIN_LIMIT_EXCEEDED);
            }
            post.pin();
        }

        return new PinPostResponse(
                post.getId(),
                post.getTitle(),
                writer.getNickname(),
                post.getType(),
                post.getCreatedAt(),
                post.getIsPinned(),
                post.getPinnedAt());
    }

    // 게시물 강제 이동
    public MovePostResponse move(Long postId, MovePostRequest request) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId).orElseThrow(
                () -> new ServiceErrorException(PostExceptionEnum.POST_NOT_FOUND));

        Board board = boardRepository.findById(request.boardId()).orElseThrow(
                () -> new ServiceErrorException(BoardExceptionEnum.BOARD_NOT_FOUND));

        User writer = userRepository.findByIdAndDeletedAtIsNull(post.getUserId()).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        boardService.validateBoardAccess(post.getUserId(), board.getId());

        post.move(board.getId());

        return new MovePostResponse(
                post.getId(),
                post.getBoardId(),
                post.getTitle(),
                writer.getNickname(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    // 게시물 강제 삭제
    public void delete(Long userId, Long postId) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId).orElseThrow(
                () -> new ServiceErrorException(PostExceptionEnum.POST_NOT_FOUND));

        User writer = userRepository.findByIdAndDeletedAtIsNull(post.getUserId()).orElse(null);
        if (writer != null) {
            writer.decreasePostCount();
        }

        post.deleteByManager(userId);

        // 댓글도 삭제 처리
        List<Comment> commentList = commentRepository.findByPostIdAndDeletedAtIsNull(postId);
        List<Long> commentUserIds = commentList.stream()
                .map(Comment::getUserId)
                .distinct()
                .toList();
        Map<Long, User> commentUserMap = userRepository.findAllByIdInAndDeletedAtIsNull(commentUserIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        for (Comment comment : commentList) {
            User commentUser = commentUserMap.get(comment.getUserId());

            comment.deleteByManager(userId);
            if (commentUser != null) commentUser.decreaseCommentCount();
        }

        // 첨부된 파일도 삭제 처리
        List<File> fileList = fileRepository.findByPostIdAndDeletedAtIsNull(postId);
        for (File file : fileList) {
            fileManagerService.delete(userId, file.getId());
        }
    }
}
