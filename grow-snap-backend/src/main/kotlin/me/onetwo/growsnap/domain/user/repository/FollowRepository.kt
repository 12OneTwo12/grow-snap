package me.onetwo.growsnap.domain.user.repository

import com.onetwo.growsnap.jooq.generated.tables.references.FOLLOWS
import me.onetwo.growsnap.domain.user.model.Follow
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

/**
 * 팔로우 Repository
 */
@Repository
class FollowRepository(
    private val dsl: DSLContext
) {
    fun save(follow: Follow): Follow {
        val record = dsl.insertInto(FOLLOWS)
            .set(FOLLOWS.FOLLOWER_ID, follow.followerId)
            .set(FOLLOWS.FOLLOWING_ID, follow.followingId)
            .returning()
            .fetchOne()!!

        return follow.copy(id = record.id)
    }

    fun delete(followerId: Long, followingId: Long) {
        dsl.deleteFrom(FOLLOWS)
            .where(FOLLOWS.FOLLOWER_ID.eq(followerId))
            .and(FOLLOWS.FOLLOWING_ID.eq(followingId))
            .execute()
    }

    fun existsByFollowerIdAndFollowingId(followerId: Long, followingId: Long): Boolean {
        return dsl.fetchExists(
            dsl.selectFrom(FOLLOWS)
                .where(FOLLOWS.FOLLOWER_ID.eq(followerId))
                .and(FOLLOWS.FOLLOWING_ID.eq(followingId))
        )
    }

    fun countByFollowerId(followerId: Long): Int {
        return dsl.fetchCount(
            dsl.selectFrom(FOLLOWS)
                .where(FOLLOWS.FOLLOWER_ID.eq(followerId))
        )
    }

    fun countByFollowingId(followingId: Long): Int {
        return dsl.fetchCount(
            dsl.selectFrom(FOLLOWS)
                .where(FOLLOWS.FOLLOWING_ID.eq(followingId))
        )
    }
}
