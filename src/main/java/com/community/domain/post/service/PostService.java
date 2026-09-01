package com.community.domain.post.service;

import com.community.common.dto.PageResponse;
import com.community.common.exception.ServiceErrorException;
import com.community.domain.comment.entity.Comment;
import com.community.domain.comment.repository.CommentRepository;
import com.community.domain.file.dto.response.GetAllFilesResponse;
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
import com.community.domain.user.enums.UserType;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import com.community.domain.user.service.UserRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostViewService postViewService;
    private final ReactionRepository reactionRepository;
    private final CommentRepository commentRepository;
    private final UserRankingService userRankingService;
    private final FileService fileService;
    private final FileRepository fileRepository;

    // 게시물 등록
    public CreatePostResponse create(Long userId, CreatePostRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        if (request.type() == PostType.NOTICE && user.getType() != UserType.ADMIN) {
            throw new ServiceErrorException(PostExceptionEnum.POST_NOTICE_FORBIDDEN);
        }

        Post post = Post.register(user.getId(), request.title(), request.content(), request.type());
        postRepository.save(post);

        fileService.attachFiles(user.getId(), post.getId(), request.fileIds());

        List<GetAllFilesResponse> files = fileService.getAll(post.getId());

        userRankingService.recordPost(user.getId());

        return new CreatePostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                user.getNickname(),
                post.getType(),
                post.getCreatedAt(),
                files
        );
    }

    // 게시물 단건 조회
    @Transactional(readOnly = true)
    public GetOnePostResponse getOne(Long postId, String clientId) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId).orElseThrow(
                () -> new ServiceErrorException(PostExceptionEnum.POST_NOT_FOUND));

        User user = userRepository.findById(post.getUserId()).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        postViewService.record(postId, clientId);

        Long commentCount = commentRepository.countByPostIdAndDeletedAtIsNull(post.getId());
        Long likeCount = reactionRepository.countByPostIdAndType(post.getId(), ReactionType.LIKE);
        Long dislikeCount = reactionRepository.countByPostIdAndType(post.getId(), ReactionType.DISLIKE);

        List<GetAllFilesResponse> files = fileService.getAll(post.getId());

        return new GetOnePostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                user.getNickname(),
                post.getType(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                commentCount,
                post.getViewCount(),
                likeCount,
                dislikeCount,
                files
        );
    }

    // 게시물 목록 조회
    @Transactional(readOnly = true)
    public PageResponse<GetAllPostsResponse> getAll(PostPageCondition condition) {
        Page<GetAllPostsResponse> page = postRepository.findPostsWithCondition(
                PageRequest.of(condition.getPage(), condition.getSize()),
                condition.getSortType(),
                condition.getKeyword(),
                condition.getSearchType(),
                null
        );

        return PageResponse.from(page);
    }

    // 내 게시물 목록 조회
    @Transactional(readOnly = true)
    public PageResponse<GetAllPostsResponse> getMine(Long userId, PostPageCondition condition) {
        Page<GetAllPostsResponse> page = postRepository.findPostsWithCondition(
                PageRequest.of(condition.getPage(), condition.getSize()),
                condition.getSortType(),
                condition.getKeyword(),
                condition.getSearchType(),
                userId
        );

        return PageResponse.from(page);
    }

    // 게시물 수정
    public UpdatePostResponse update(Long userId, Long postId, UpdatePostRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        Post post = postRepository.findByIdAndDeletedAtIsNull(postId).orElseThrow(
                () -> new ServiceErrorException(PostExceptionEnum.POST_NOT_FOUND));

        if (!post.getUserId().equals(user.getId())) {
            throw new ServiceErrorException(PostExceptionEnum.POST_FORBIDDEN);
        }

        post.update(request);

        fileService.updateFiles(user.getId(), post.getId(), request.fileIds());

        return new UpdatePostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                user.getNickname(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    // 게시물 삭제
    public void delete(Long userId, Long postId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        Post post = postRepository.findByIdAndDeletedAtIsNull(postId).orElseThrow(
                () -> new ServiceErrorException(PostExceptionEnum.POST_NOT_FOUND));

        if (!post.getUserId().equals(user.getId())) {
            throw new ServiceErrorException(PostExceptionEnum.POST_FORBIDDEN);
        }

        post.delete();
        user.decreasePostCount();

        // 댓글도 삭제 처리
        List<Comment> commentList = commentRepository.findByPostIdAndDeletedAtIsNull(postId);
        for (Comment comment : commentList) {
            User commentUser = userRepository.findByIdAndDeletedAtIsNull(comment.getUserId()).orElse(null);

            comment.delete();
            if (commentUser != null) commentUser.decreaseCommentCount();
        }

        // 첨부된 파일도 삭제 처리
        List<File> fileList = fileRepository.findByPostIdAndDeletedAtIsNull(postId);
        for (File file : fileList) {
            file.delete();
        }
    }
}
