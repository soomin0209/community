package com.community.domain.comment.repository;

import com.community.domain.comment.dto.response.CommentGetAllResponse;
import com.community.domain.comment.dto.response.CommentGetMineResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.community.domain.comment.entity.QComment.comment;
import static com.community.domain.post.entity.QPost.post;
import static com.community.domain.user.entity.QUser.user;

@RequiredArgsConstructor
public class CommentCustomRepositoryImpl implements CommentCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<CommentGetAllResponse> findCommentsWithCondition(Pageable pageable, Long postId) {
        List<CommentGetAllResponse> list = queryFactory
                .select(Projections.constructor(CommentGetAllResponse.class,
                        comment.id,
                        user.nickname,
                        comment.content,
                        comment.createdAt))
                .from(comment)
                .join(user).on(comment.userId.eq(user.id))
                .where(
                        comment.deletedAt.isNull(),
                        comment.postId.eq(postId)
                )
                .orderBy(comment.createdAt.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(comment.count())
                .from(comment)
                .join(user).on(comment.userId.eq(user.id))
                .where(
                        comment.deletedAt.isNull(),
                        comment.postId.eq(postId)
                )
                .fetchOne();

        if (total == null) total = 0L;

        return new PageImpl<>(list, pageable, total);
    }

    @Override
    public Page<CommentGetMineResponse> findMyCommentsWithCondition(Pageable pageable, Long userId) {
        List<CommentGetMineResponse> list = queryFactory
                .select(Projections.constructor(CommentGetMineResponse.class,
                        comment.id,
                        post.id,
                        post.title,
                        comment.content,
                        comment.createdAt))
                .from(comment)
                .join(post).on(comment.postId.eq(post.id))
                .where(
                        comment.deletedAt.isNull(),
                        comment.userId.eq(userId)
                )
                .orderBy(comment.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(comment.count())
                .from(comment)
                .join(post).on(comment.postId.eq(post.id))
                .where(
                        comment.deletedAt.isNull(),
                        comment.userId.eq(userId)
                )
                .fetchOne();

        if (total == null) total = 0L;

        return new PageImpl<>(list, pageable, total);
    }
}
