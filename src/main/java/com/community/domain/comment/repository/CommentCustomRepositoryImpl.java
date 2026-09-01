package com.community.domain.comment.repository;

import com.community.domain.comment.dto.response.GetAllCommentsResponse;
import com.community.domain.comment.dto.response.GetMyCommentsResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
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
    public List<GetAllCommentsResponse> findParentCommentsWithCursor(Long cursor, int size, Long postId) {
        return queryFactory
                .select(Projections.constructor(GetAllCommentsResponse.class,
                        comment.id,
                        comment.parentId,
                        new CaseBuilder()
                                .when(comment.deletedAt.isNotNull())
                                .then("알 수 없음")
                                .otherwise(user.nickname),
                        comment.userId.eq(post.userId),
                        new CaseBuilder()
                                .when(comment.deletedAt.isNotNull())
                                .then("삭제된 댓글입니다")
                                .otherwise(comment.content),
                        comment.createdAt,
                        comment.depth))
                .from(comment)
                .join(user).on(comment.userId.eq(user.id))
                .join(post).on(comment.postId.eq(post.id))
                .where(
                        comment.postId.eq(postId),
                        comment.parentId.isNull(),
                        cursor != null ? comment.id.gt(cursor) : null
                )
                .orderBy(comment.id.asc())
                .limit(size + 1)    // hasNext 판단을 위해 +1
                .fetch();
    }

    @Override
    public List<GetAllCommentsResponse> findChildCommentsByParentIds(List<Long> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) return List.of();

        return queryFactory
                .select(Projections.constructor(GetAllCommentsResponse.class,
                        comment.id,
                        comment.parentId,
                        new CaseBuilder()
                                .when(comment.deletedAt.isNotNull())
                                .then("알 수 없음")
                                .otherwise(user.nickname),
                        comment.userId.eq(post.userId),
                        new CaseBuilder()
                                .when(comment.deletedAt.isNotNull())
                                .then("삭제된 댓글입니다")
                                .otherwise(comment.content),
                        comment.createdAt,
                        comment.depth))
                .from(comment)
                .join(user).on(comment.userId.eq(user.id))
                .join(post).on(comment.postId.eq(post.id))
                .where(
                        comment.parentId.in(parentIds)
                )
                .orderBy(comment.createdAt.asc())
                .fetch();
    }

    @Override
    public Page<GetMyCommentsResponse> findMyCommentsWithCondition(Pageable pageable, Long userId) {
        List<GetMyCommentsResponse> list = queryFactory
                .select(Projections.constructor(GetMyCommentsResponse.class,
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
