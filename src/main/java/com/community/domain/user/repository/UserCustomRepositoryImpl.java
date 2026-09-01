package com.community.domain.user.repository;

import com.community.domain.user.dto.UserCountProjection;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.community.domain.comment.entity.QComment.comment;
import static com.community.domain.post.entity.QPost.post;
import static com.community.domain.user.entity.QUser.user;

@RequiredArgsConstructor
public class UserCustomRepositoryImpl implements UserCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<UserCountProjection> findUserCounts() {
        return queryFactory
                .select(Projections.constructor(UserCountProjection.class,
                        user.id,
                        post.id.countDistinct().coalesce(0L),
                        comment.id.countDistinct().coalesce(0L)))
                .from(user)
                .leftJoin(post).on(post.userId.eq(user.id).and(post.deletedAt.isNull()))
                .leftJoin(comment).on(comment.userId.eq(user.id).and(comment.deletedAt.isNull()))
                .where(user.deletedAt.isNull())
                .groupBy(user.id)
                .fetch();
    }
}
