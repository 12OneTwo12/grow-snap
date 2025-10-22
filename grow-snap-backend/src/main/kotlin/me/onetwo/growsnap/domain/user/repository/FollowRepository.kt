package me.onetwo.growsnap.domain.user.repository

import me.onetwo.growsnap.jooq.generated.tables.references.FOLLOWS
import me.onetwo.growsnap.domain.user.model.Follow
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

/**
 * 팔로우 Repository
 */
@Repository
class FollowRepository(
    private val dsl: DSLContext
) {
    fun save(follow: Follow): Follow {
        val record = dsl.insertInto(FOLLOWS)
            .set(FOLLOWS.FOLLOWER_ID, follow.followerId.toString())
            .set(FOLLOWS.FOLLOWING_ID, follow.followingId.toString())
            .returning()
            .fetchOne()!!

        return follow.copy(id = record.id)
    }

    /**
     * 팔로우 삭제 (Soft Delete)
     *
     * @param followerId 팔로워 ID
     * @param followingId 팔로잉 ID
     */
    fun softDelete(followerId: UUID, followingId: UUID) {
        dsl.update(FOLLOWS)
            .set(FOLLOWS.DELETED_AT, LocalDateTime.now())
            .set(FOLLOWS.UPDATED_AT, LocalDateTime.now())
            .set(FOLLOWS.UPDATED_BY, followerId.toString())  // 언팔로우를 수행한 사용자
            .where(FOLLOWS.FOLLOWER_ID.eq(followerId.toString()))
            .and(FOLLOWS.FOLLOWING_ID.eq(followingId.toString()))
            .and(FOLLOWS.DELETED_AT.isNull)  // 이미 삭제된 데이터는 제외
            .execute()
    }

    fun existsByFollowerIdAndFollowingId(followerId: UUID, followingId: UUID): Boolean {
        return dsl.fetchExists(
            dsl.selectFrom(FOLLOWS)
                .where(FOLLOWS.FOLLOWER_ID.eq(followerId.toString()))
                .and(FOLLOWS.FOLLOWING_ID.eq(followingId.toString()))
                .and(FOLLOWS.DELETED_AT.isNull)  // Soft delete 필터링
        )
    }

    fun countByFollowerId(followerId: UUID): Int {
        return dsl.fetchCount(
            dsl.selectFrom(FOLLOWS)
                .where(FOLLOWS.FOLLOWER_ID.eq(followerId.toString()))
                .and(FOLLOWS.DELETED_AT.isNull)  // Soft delete 필터링
        )
    }

    fun countByFollowingId(followingId: UUID): Int {
        return dsl.fetchCount(
            dsl.selectFrom(FOLLOWS)
                .where(FOLLOWS.FOLLOWING_ID.eq(followingId.toString()))
                .and(FOLLOWS.DELETED_AT.isNull)  // Soft delete 필터링
        )
    }

    /**
     * 사용자의 모든 팔로우 관계 soft delete (탈퇴 시 사용)
     *
     * 사용자가 팔로우한 관계와 사용자를 팔로우한 관계 모두 삭제합니다.
     *
     * @param userId 사용자 ID
     * @param deletedBy 삭제한 사용자 ID
     */
    fun softDeleteAllByUserId(userId: UUID, deletedBy: UUID) {
        val now = LocalDateTime.now()

        // 사용자가 팔로우한 관계 삭제
        dsl.update(FOLLOWS)
            .set(FOLLOWS.DELETED_AT, now)
            .set(FOLLOWS.UPDATED_AT, now)
            .set(FOLLOWS.UPDATED_BY, deletedBy.toString())
            .where(FOLLOWS.FOLLOWER_ID.eq(userId.toString()))
            .and(FOLLOWS.DELETED_AT.isNull)
            .execute()

        // 사용자를 팔로우한 관계 삭제
        dsl.update(FOLLOWS)
            .set(FOLLOWS.DELETED_AT, now)
            .set(FOLLOWS.UPDATED_AT, now)
            .set(FOLLOWS.UPDATED_BY, deletedBy.toString())
            .where(FOLLOWS.FOLLOWING_ID.eq(userId.toString()))
            .and(FOLLOWS.DELETED_AT.isNull)
            .execute()
    }
}
