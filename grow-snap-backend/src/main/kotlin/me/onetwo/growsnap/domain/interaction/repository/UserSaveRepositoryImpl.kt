package me.onetwo.growsnap.domain.interaction.repository

import me.onetwo.growsnap.domain.interaction.model.UserSave
import me.onetwo.growsnap.jooq.generated.tables.UserSaves.Companion.USER_SAVES
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.UUID

@Repository
class UserSaveRepositoryImpl(
    private val dslContext: DSLContext
) : UserSaveRepository {

    override fun save(userId: UUID, contentId: UUID): Mono<UserSave> {
        return Mono.fromCallable {
            val now = LocalDateTime.now()

            dslContext
                .insertInto(USER_SAVES)
                .set(USER_SAVES.USER_ID, userId.toString())
                .set(USER_SAVES.CONTENT_ID, contentId.toString())
                .set(USER_SAVES.CREATED_AT, now)
                .set(USER_SAVES.CREATED_BY, userId.toString())
                .set(USER_SAVES.UPDATED_AT, now)
                .set(USER_SAVES.UPDATED_BY, userId.toString())
                .returning()
                .fetchOne()
                ?.let {
                    UserSave(
                        id = it.getValue(USER_SAVES.ID),
                        userId = UUID.fromString(it.getValue(USER_SAVES.USER_ID)),
                        contentId = UUID.fromString(it.getValue(USER_SAVES.CONTENT_ID)),
                        createdAt = it.getValue(USER_SAVES.CREATED_AT)!!,
                        createdBy = it.getValue(USER_SAVES.CREATED_BY)?.let { UUID.fromString(it) },
                        updatedAt = it.getValue(USER_SAVES.UPDATED_AT)!!,
                        updatedBy = it.getValue(USER_SAVES.UPDATED_BY)?.let { UUID.fromString(it) },
                        deletedAt = it.getValue(USER_SAVES.DELETED_AT)
                    )
                } ?: throw IllegalStateException("Failed to create user save")
        }
    }

    override fun delete(userId: UUID, contentId: UUID): Mono<Void> {
        return Mono.fromCallable {
            val now = LocalDateTime.now()

            dslContext
                .update(USER_SAVES)
                .set(USER_SAVES.DELETED_AT, now)
                .set(USER_SAVES.UPDATED_AT, now)
                .set(USER_SAVES.UPDATED_BY, userId.toString())
                .where(USER_SAVES.USER_ID.eq(userId.toString()))
                .and(USER_SAVES.CONTENT_ID.eq(contentId.toString()))
                .and(USER_SAVES.DELETED_AT.isNull)
                .execute()
        }.then()
    }

    override fun exists(userId: UUID, contentId: UUID): Mono<Boolean> {
        return Mono.fromCallable {
            dslContext
                .selectCount()
                .from(USER_SAVES)
                .where(USER_SAVES.USER_ID.eq(userId.toString()))
                .and(USER_SAVES.CONTENT_ID.eq(contentId.toString()))
                .and(USER_SAVES.DELETED_AT.isNull)
                .fetchOne(0, Int::class.java) ?: 0 > 0
        }
    }

    override fun findByUserId(userId: UUID): Flux<UserSave> {
        return Flux.defer {
            Flux.fromIterable(
                dslContext
                    .select(
                        USER_SAVES.ID,
                        USER_SAVES.USER_ID,
                        USER_SAVES.CONTENT_ID,
                        USER_SAVES.CREATED_AT,
                        USER_SAVES.CREATED_BY,
                        USER_SAVES.UPDATED_AT,
                        USER_SAVES.UPDATED_BY,
                        USER_SAVES.DELETED_AT
                    )
                    .from(USER_SAVES)
                    .where(USER_SAVES.USER_ID.eq(userId.toString()))
                    .and(USER_SAVES.DELETED_AT.isNull)
                    .orderBy(USER_SAVES.CREATED_AT.desc())
                    .fetch()
                    .map {
                        UserSave(
                            id = it.getValue(USER_SAVES.ID),
                            userId = UUID.fromString(it.getValue(USER_SAVES.USER_ID)),
                            contentId = UUID.fromString(it.getValue(USER_SAVES.CONTENT_ID)),
                            createdAt = it.getValue(USER_SAVES.CREATED_AT)!!,
                            createdBy = it.getValue(USER_SAVES.CREATED_BY)?.let { UUID.fromString(it) },
                            updatedAt = it.getValue(USER_SAVES.UPDATED_AT)!!,
                            updatedBy = it.getValue(USER_SAVES.UPDATED_BY)?.let { UUID.fromString(it) },
                            deletedAt = it.getValue(USER_SAVES.DELETED_AT)
                        )
                    }
            )
        }
    }
}
