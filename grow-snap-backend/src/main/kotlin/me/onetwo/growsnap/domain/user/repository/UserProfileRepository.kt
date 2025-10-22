package me.onetwo.growsnap.domain.user.repository

import me.onetwo.growsnap.jooq.generated.tables.references.USER_PROFILES
import me.onetwo.growsnap.domain.user.model.UserProfile
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

/**
 * 사용자 프로필 Repository
 */
@Repository
class UserProfileRepository(
    private val dsl: DSLContext
) {
    fun save(profile: UserProfile): UserProfile {
        val record = dsl.insertInto(USER_PROFILES)
            .set(USER_PROFILES.USER_ID, profile.userId)
            .set(USER_PROFILES.NICKNAME, profile.nickname)
            .set(USER_PROFILES.PROFILE_IMAGE_URL, profile.profileImageUrl)
            .set(USER_PROFILES.BIO, profile.bio)
            .returning()
            .fetchOne()!!

        return profile.copy(id = record.id)
    }

    fun findByUserId(userId: Long): UserProfile? {
        return dsl.selectFrom(USER_PROFILES)
            .where(USER_PROFILES.USER_ID.eq(userId))
            .fetchOne()
            ?.let { mapToUserProfile(it) }
    }

    fun findByNickname(nickname: String): UserProfile? {
        return dsl.selectFrom(USER_PROFILES)
            .where(USER_PROFILES.NICKNAME.eq(nickname))
            .fetchOne()
            ?.let { mapToUserProfile(it) }
    }

    fun update(profile: UserProfile): UserProfile {
        dsl.update(USER_PROFILES)
            .set(USER_PROFILES.NICKNAME, profile.nickname)
            .set(USER_PROFILES.PROFILE_IMAGE_URL, profile.profileImageUrl)
            .set(USER_PROFILES.BIO, profile.bio)
            .set(USER_PROFILES.FOLLOWER_COUNT, profile.followerCount)
            .set(USER_PROFILES.FOLLOWING_COUNT, profile.followingCount)
            .where(USER_PROFILES.USER_ID.eq(profile.userId))
            .execute()

        return profile
    }

    fun existsByNickname(nickname: String): Boolean {
        return dsl.fetchExists(
            dsl.selectFrom(USER_PROFILES)
                .where(USER_PROFILES.NICKNAME.eq(nickname))
        )
    }

    private fun mapToUserProfile(record: me.onetwo.growsnap.jooq.generated.tables.records.UserProfilesRecord): UserProfile {
        return UserProfile(
            id = record.id,
            userId = record.userId!!,
            nickname = record.nickname!!,
            profileImageUrl = record.profileImageUrl,
            bio = record.bio,
            followerCount = record.followerCount ?: 0,
            followingCount = record.followingCount ?: 0,
            createdAt = record.createdAt!!,
            updatedAt = record.updatedAt!!
        )
    }
}
