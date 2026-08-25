package com.community.domain.comment.repository;

import com.community.domain.comment.dto.response.CommentGetAllResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.community.domain.comment.entity.QComment.comment;
import static com.community.domain.user.entity.QUser.user;

@RequiredArgsConstructor
public class CommentCustomRepositoryImpl implements CommentCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<CommentGetAllResponse> findCommentsWithCondition(Pageable pageable, Long postId, Long userId) {
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
                        postIdCondition(postId),
                        userIdCondition(userId)
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
                        postIdCondition(postId),
                        userIdCondition(userId)
                )
                .fetchOne();

        if (total == null) total = 0L;

        return new PageImpl<>(list, pageable, total);
    }

    private BooleanExpression postIdCondition(Long postId) {
        return postId != null ? comment.postId.eq(postId) : null;
    }

    private BooleanExpression userIdCondition(Long userId) {
        return userId != null ? comment.userId.eq(userId) : null;
    }
}
