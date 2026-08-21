package com.community.domain.post.repository;

import com.community.domain.post.dto.response.PostGetAllResponse;
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

import static com.community.domain.post.entity.QPost.post;
import static com.community.domain.user.entity.QUser.user;

@RequiredArgsConstructor
public class PostCustomRepositoryImpl implements PostCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<PostGetAllResponse> findPostsWithCondition(
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
        return getPostsWithNotices(pageable, sortType);
    }

    // 통합 검색
    private Page<PostGetAllResponse> searchAllPosts(
            Pageable pageable,
            PostSortType sortType,
            String keyword,
            PostSearchType searchType,
            Long userId
    ) {
        List<PostGetAllResponse> list = queryFactory
                .select(Projections.constructor(PostGetAllResponse.class,
                        post.id,
                        post.title,
                        user.nickname,
                        post.type,
                        post.createdAt))
                .from(post)
                .join(user).on(post.userId.eq(user.id))
                .where(
                        post.deletedAt.isNull(),
                        searchCondition(keyword, searchType),
                        userIdCondition(userId)
                )
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

    // 공지글 분리 조회
    private Page<PostGetAllResponse> getPostsWithNotices(
            Pageable pageable,
            PostSortType sortType
    ) {
        List<PostGetAllResponse> result = new ArrayList<>();
        int noticeCount = 0;

        // 첫 페이지에 공지글 추가
        if (pageable.getPageNumber() == 0) {
            List<PostGetAllResponse> notices = queryFactory
                    .select(Projections.constructor(PostGetAllResponse.class,
                            post.id,
                            post.title,
                            user.nickname,
                            post.type,
                            post.createdAt))
                    .from(post)
                    .join(user).on(post.userId.eq(user.id))
                    .where(
                            post.deletedAt.isNull(),
                            post.type.eq(PostType.NOTICE)
                    )
                    .orderBy(getOrderSpecifier(sortType))
                    .limit(10)
                    .fetch();

            result.addAll(notices);
            noticeCount = notices.size();
        }

        // 공지글 제외 게시물 조회
        int limit = pageable.getPageSize() - noticeCount;

        List<PostGetAllResponse> posts = queryFactory
                .select(Projections.constructor(PostGetAllResponse.class,
                        post.id,
                        post.title,
                        user.nickname,
                        post.type,
                        post.createdAt))
                .from(post)
                .join(user).on(post.userId.eq(user.id))
                .where(
                        post.deletedAt.isNull(),
                        post.type.ne(PostType.NOTICE)   // 공지글 제외
                )
                .orderBy(getOrderSpecifier(sortType))
                .offset(pageable.getOffset())
                .limit(limit)
                .fetch();

        result.addAll(posts);

        Long total = queryFactory
                .select(post.count())
                .from(post)
                .join(user).on(post.userId.eq(user.id))
                .where(
                        post.deletedAt.isNull(),
                        post.type.ne(PostType.NOTICE)
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
