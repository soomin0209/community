package com.community.domain.post.repository;

import com.community.domain.post.dto.response.GetAllPostsResponse;
import com.community.domain.post.enums.PostSearchType;
import com.community.domain.post.enums.PostSortType;
import com.community.domain.post.enums.PostType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

import static com.community.domain.comment.entity.QComment.comment;
import static com.community.domain.post.entity.QPost.post;
import static com.community.domain.user.entity.QUser.user;

@RequiredArgsConstructor
public class PostCustomRepositoryImpl implements PostCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<GetAllPostsResponse> findPostsWithCondition(
            Pageable pageable,
            PostSortType sortType,
            String keyword,
            PostSearchType searchType,
            Long userId
    ) {
        // 검색이거나 내 게시물 조회일 경우
        if (keyword != null && !keyword.isBlank() || userId != null) {
            return searchAllPosts(pageable, sortType, keyword, searchType, userId);
        }

        // 일반 목록 조회일 경우
        return getPostsWithPinned(pageable, sortType);
    }

    // 통합 검색
    private Page<GetAllPostsResponse> searchAllPosts(
            Pageable pageable,
            PostSortType sortType,
            String keyword,
            PostSearchType searchType,
            Long userId
    ) {
        List<GetAllPostsResponse> list = queryFactory
                .select(Projections.constructor(GetAllPostsResponse.class,
                        post.id,
                        post.title,
                        user.nickname,
                        post.type,
                        post.isPinned,
                        post.createdAt,
                        comment.count(),
                        post.viewCount))
                .from(post)
                .join(user).on(post.userId.eq(user.id))
                .leftJoin(comment).on(comment.postId.eq(post.id).and(comment.deletedAt.isNull()))
                .where(
                        post.deletedAt.isNull(),
                        searchCondition(keyword, searchType),
                        userIdCondition(userId)
                )
                .groupBy(post.id)
                .orderBy(getOrderSpecifier(sortType))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(post.count())
                .from(post)
                .join(user).on(post.userId.eq(user.id))
                .where(
                        post.deletedAt.isNull(),
                        searchCondition(keyword, searchType),
                        userIdCondition(userId)
                )
                .fetchOne();

        if (total == null) total = 0L;

        return new PageImpl<>(list, pageable, total);
    }

    // 고정글 분리 조회
    private Page<GetAllPostsResponse> getPostsWithPinned(
            Pageable pageable,
            PostSortType sortType
    ) {
        List<GetAllPostsResponse> result = new ArrayList<>();
        int noticeCount = 0;

        // 첫 페이지에 고정글 추가
        if (pageable.getPageNumber() == 0) {
            List<GetAllPostsResponse> pinned = queryFactory
                    .select(Projections.constructor(GetAllPostsResponse.class,
                            post.id,
                            post.title,
                            user.nickname,
                            post.type,
                            post.isPinned,
                            post.createdAt,
                            comment.count(),
                            post.viewCount))
                    .from(post)
                    .join(user).on(post.userId.eq(user.id))
                    .leftJoin(comment).on(comment.postId.eq(post.id).and(comment.deletedAt.isNull()))
                    .where(
                            post.deletedAt.isNull(),
                            post.isPinned.isTrue()
                    )
                    .groupBy(post.id)
                    .orderBy(post.pinnedAt.desc())
                    .limit(10)
                    .fetch();

            result.addAll(pinned);
            noticeCount = pinned.size();
        }

        // 고정글 제외 게시물 조회
        int limit = pageable.getPageSize() - noticeCount;

        List<GetAllPostsResponse> unpinned = queryFactory
                .select(Projections.constructor(GetAllPostsResponse.class,
                        post.id,
                        post.title,
                        user.nickname,
                        post.type,
                        post.isPinned,
                        post.createdAt,
                        comment.count(),
                        post.viewCount))
                .from(post)
                .join(user).on(post.userId.eq(user.id))
                .leftJoin(comment).on(comment.postId.eq(post.id).and(comment.deletedAt.isNull()))
                .where(
                        post.deletedAt.isNull(),
                        post.isPinned.isFalse()
                )
                .groupBy(post.id)
                .orderBy(getOrderSpecifier(sortType))
                .offset(pageable.getOffset())
                .limit(limit)
                .fetch();

        result.addAll(unpinned);

        Long total = queryFactory
                .select(post.count())
                .from(post)
                .join(user).on(post.userId.eq(user.id))
                .where(
                        post.deletedAt.isNull(),
                        post.isPinned.isFalse()
                )
                .fetchOne();

        if (total == null) total = 0L;

        return new PageImpl<>(result, pageable, total);
    }

    private OrderSpecifier<?> getOrderSpecifier(PostSortType sortType) {
        return switch (sortType) {
            case LATEST -> post.createdAt.desc();
            case OLDEST -> post.createdAt.asc();
        };
    }

    private BooleanExpression searchCondition(String keyword, PostSearchType searchType) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return switch (searchType) {
            case AUTHOR -> user.nickname.containsIgnoreCase(keyword);
            case TITLE -> post.title.containsIgnoreCase(keyword);
            case CONTENT -> post.content.containsIgnoreCase(keyword);
            case TITLE_CONTENT -> post.title.containsIgnoreCase(keyword)
                    .or(post.content.containsIgnoreCase(keyword));
        };
    }

    private BooleanExpression userIdCondition(Long userId) {
        return userId != null ? post.userId.eq(userId) : null;
    }
}
