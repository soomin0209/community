package com.community.domain.post.repository;

import com.community.domain.post.dto.response.PostGetAllResponse;
import com.community.domain.post.enums.PostSearchType;
import com.community.domain.post.enums.PostSortType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
        List<PostGetAllResponse> list = queryFactory
                .select(Projections.constructor(PostGetAllResponse.class,
                        post.id,
                        post.title,
                        user.nickname,
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
