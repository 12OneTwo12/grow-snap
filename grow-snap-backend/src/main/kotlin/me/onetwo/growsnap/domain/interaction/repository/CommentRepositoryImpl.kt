package me.onetwo.growsnap.domain.interaction.repository

import me.onetwo.growsnap.domain.interaction.model.Comment
import me.onetwo.growsnap.jooq.generated.tables.Comments.Companion.COMMENTS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.UUID

@Repository
class CommentRepositoryImpl(
    private val dslContext: DSLContext
) : CommentRepository {

    override fun save(comment: Comment): Mono<Comment> {
        return Mono.fromCallable {
            val now = LocalDateTime.now()
            val commentId = comment.id ?: UUID.randomUUID()

            dslContext
                .insertInto(COMMENTS)
                .set(COMMENTS.ID, commentId.toString())
                .set(COMMENTS.CONTENT_ID, comment.contentId.toString())
                .set(COMMENTS.USER_ID, comment.userId.toString())
                .set(COMMENTS.PARENT_COMMENT_ID, comment.parentCommentId?.toString())
                .set(COMMENTS.CONTENT, comment.content)
                .set(COMMENTS.TIMESTAMP_SECONDS, comment.timestampSeconds)
                .set(COMMENTS.CREATED_AT, now)
                .set(COMMENTS.CREATED_BY, comment.userId.toString())
                .set(COMMENTS.UPDATED_AT, now)
                .set(COMMENTS.UPDATED_BY, comment.userId.toString())
                .returning()
                .fetchOne()
                ?.let {
                    Comment(
                        id = UUID.fromString(it.getValue(COMMENTS.ID)),
                        contentId = UUID.fromString(it.getValue(COMMENTS.CONTENT_ID)),
                        userId = UUID.fromString(it.getValue(COMMENTS.USER_ID)),
                        parentCommentId = it.getValue(COMMENTS.PARENT_COMMENT_ID)?.let { UUID.fromString(it) },
                        content = it.getValue(COMMENTS.CONTENT)!!,
                        timestampSeconds = it.getValue(COMMENTS.TIMESTAMP_SECONDS),
                        createdAt = it.getValue(COMMENTS.CREATED_AT)!!,
                        createdBy = it.getValue(COMMENTS.CREATED_BY)?.let { UUID.fromString(it) },
                        updatedAt = it.getValue(COMMENTS.UPDATED_AT)!!,
                        updatedBy = it.getValue(COMMENTS.UPDATED_BY)?.let { UUID.fromString(it) },
                        deletedAt = it.getValue(COMMENTS.DELETED_AT)
                    )
                } ?: throw IllegalStateException("Failed to create comment")
        }
    }

    override fun findById(commentId: UUID): Mono<Comment> {
        return Mono.fromCallable {
            dslContext
                .select(
                    COMMENTS.ID,
                    COMMENTS.CONTENT_ID,
                    COMMENTS.USER_ID,
                    COMMENTS.PARENT_COMMENT_ID,
                    COMMENTS.CONTENT,
                    COMMENTS.TIMESTAMP_SECONDS,
                    COMMENTS.CREATED_AT,
                    COMMENTS.CREATED_BY,
                    COMMENTS.UPDATED_AT,
                    COMMENTS.UPDATED_BY,
                    COMMENTS.DELETED_AT
                )
                .from(COMMENTS)
                .where(COMMENTS.ID.eq(commentId.toString()))
                .and(COMMENTS.DELETED_AT.isNull)
                .fetchOne()
                ?.let {
                    Comment(
                        id = UUID.fromString(it.getValue(COMMENTS.ID)),
                        contentId = UUID.fromString(it.getValue(COMMENTS.CONTENT_ID)),
                        userId = UUID.fromString(it.getValue(COMMENTS.USER_ID)),
                        parentCommentId = it.getValue(COMMENTS.PARENT_COMMENT_ID)?.let { UUID.fromString(it) },
                        content = it.getValue(COMMENTS.CONTENT)!!,
                        timestampSeconds = it.getValue(COMMENTS.TIMESTAMP_SECONDS),
                        createdAt = it.getValue(COMMENTS.CREATED_AT)!!,
                        createdBy = it.getValue(COMMENTS.CREATED_BY)?.let { UUID.fromString(it) },
                        updatedAt = it.getValue(COMMENTS.UPDATED_AT)!!,
                        updatedBy = it.getValue(COMMENTS.UPDATED_BY)?.let { UUID.fromString(it) },
                        deletedAt = it.getValue(COMMENTS.DELETED_AT)
                    )
                }
        }.flatMap { Mono.justOrEmpty(it) }
    }

    override fun findByContentId(contentId: UUID): Flux<Comment> {
        return Flux.defer {
            Flux.fromIterable(
                dslContext
                    .select(
                        COMMENTS.ID,
                        COMMENTS.CONTENT_ID,
                        COMMENTS.USER_ID,
                        COMMENTS.PARENT_COMMENT_ID,
                        COMMENTS.CONTENT,
                        COMMENTS.TIMESTAMP_SECONDS,
                        COMMENTS.CREATED_AT,
                        COMMENTS.CREATED_BY,
                        COMMENTS.UPDATED_AT,
                        COMMENTS.UPDATED_BY,
                        COMMENTS.DELETED_AT
                    )
                    .from(COMMENTS)
                    .where(COMMENTS.CONTENT_ID.eq(contentId.toString()))
                    .and(COMMENTS.DELETED_AT.isNull)
                    .orderBy(COMMENTS.CREATED_AT.asc())
                    .fetch()
                    .map {
                        Comment(
                            id = UUID.fromString(it.getValue(COMMENTS.ID)),
                            contentId = UUID.fromString(it.getValue(COMMENTS.CONTENT_ID)),
                            userId = UUID.fromString(it.getValue(COMMENTS.USER_ID)),
                            parentCommentId = it.getValue(COMMENTS.PARENT_COMMENT_ID)?.let { UUID.fromString(it) },
                            content = it.getValue(COMMENTS.CONTENT)!!,
                            timestampSeconds = it.getValue(COMMENTS.TIMESTAMP_SECONDS),
                            createdAt = it.getValue(COMMENTS.CREATED_AT)!!,
                            createdBy = it.getValue(COMMENTS.CREATED_BY)?.let { UUID.fromString(it) },
                            updatedAt = it.getValue(COMMENTS.UPDATED_AT)!!,
                            updatedBy = it.getValue(COMMENTS.UPDATED_BY)?.let { UUID.fromString(it) },
                            deletedAt = it.getValue(COMMENTS.DELETED_AT)
                        )
                    }
            )
        }
    }

    override fun findByParentCommentId(parentCommentId: UUID): Flux<Comment> {
        return Flux.defer {
            Flux.fromIterable(
                dslContext
                    .select(
                        COMMENTS.ID,
                        COMMENTS.CONTENT_ID,
                        COMMENTS.USER_ID,
                        COMMENTS.PARENT_COMMENT_ID,
                        COMMENTS.CONTENT,
                        COMMENTS.TIMESTAMP_SECONDS,
                        COMMENTS.CREATED_AT,
                        COMMENTS.CREATED_BY,
                        COMMENTS.UPDATED_AT,
                        COMMENTS.UPDATED_BY,
                        COMMENTS.DELETED_AT
                    )
                    .from(COMMENTS)
                    .where(COMMENTS.PARENT_COMMENT_ID.eq(parentCommentId.toString()))
                    .and(COMMENTS.DELETED_AT.isNull)
                    .orderBy(COMMENTS.CREATED_AT.asc())
                    .fetch()
                    .map {
                        Comment(
                            id = UUID.fromString(it.getValue(COMMENTS.ID)),
                            contentId = UUID.fromString(it.getValue(COMMENTS.CONTENT_ID)),
                            userId = UUID.fromString(it.getValue(COMMENTS.USER_ID)),
                            parentCommentId = it.getValue(COMMENTS.PARENT_COMMENT_ID)?.let { UUID.fromString(it) },
                            content = it.getValue(COMMENTS.CONTENT)!!,
                            timestampSeconds = it.getValue(COMMENTS.TIMESTAMP_SECONDS),
                            createdAt = it.getValue(COMMENTS.CREATED_AT)!!,
                            createdBy = it.getValue(COMMENTS.CREATED_BY)?.let { UUID.fromString(it) },
                            updatedAt = it.getValue(COMMENTS.UPDATED_AT)!!,
                            updatedBy = it.getValue(COMMENTS.UPDATED_BY)?.let { UUID.fromString(it) },
                            deletedAt = it.getValue(COMMENTS.DELETED_AT)
                        )
                    }
            )
        }
    }

    override fun delete(commentId: UUID, userId: UUID): Mono<Void> {
        return Mono.fromCallable {
            val now = LocalDateTime.now()

            dslContext
                .update(COMMENTS)
                .set(COMMENTS.DELETED_AT, now)
                .set(COMMENTS.UPDATED_AT, now)
                .set(COMMENTS.UPDATED_BY, userId.toString())
                .where(COMMENTS.ID.eq(commentId.toString()))
                .and(COMMENTS.DELETED_AT.isNull)
                .execute()
        }.then()
    }
}
