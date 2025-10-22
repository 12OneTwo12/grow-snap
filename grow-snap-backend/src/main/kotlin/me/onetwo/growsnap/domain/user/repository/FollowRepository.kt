package me.onetwo.growsnap.domain.user.repository

import me.onetwo.growsnap.jooq.generated.tables.references.FOLLOWS
import me.onetwo.growsnap.domain.user.model.Follow
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
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

    fun delete(followerId: UUID, followingId: UUID) {
        dsl.deleteFrom(FOLLOWS)
            .where(FOLLOWS.FOLLOWER_ID.eq(followerId.toString()))
            .and(FOLLOWS.FOLLOWING_ID.eq(followingId.toString()))
            .execute()
    }

    fun existsByFollowerIdAndFollowingId(followerId: UUID, followingId: UUID): Boolean {
        return dsl.fetchExists(
            dsl.selectFrom(FOLLOWS)
                .where(FOLLOWS.FOLLOWER_ID.eq(followerId.toString()))
                .and(FOLLOWS.FOLLOWING_ID.eq(followingId.toString()))
        )
    }

    fun countByFollowerId(followerId: UUID): Int {
        return dsl.fetchCount(
            dsl.selectFrom(FOLLOWS)
                .where(FOLLOWS.FOLLOWER_ID.eq(followerId.toString()))
        )
    }

    fun countByFollowingId(followingId: UUID): Int {
        return dsl.fetchCount(
            dsl.selectFrom(FOLLOWS)
                .where(FOLLOWS.FOLLOWING_ID.eq(followingId.toString()))
        )
    }
}
