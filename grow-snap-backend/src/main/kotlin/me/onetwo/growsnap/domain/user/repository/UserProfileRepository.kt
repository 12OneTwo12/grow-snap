package me.onetwo.growsnap.domain.user.repository

import me.onetwo.growsnap.jooq.generated.tables.references.USER_PROFILES
import me.onetwo.growsnap.domain.user.model.UserProfile
import me.onetwo.growsnap.jooq.generated.tables.records.UserProfilesRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * 사용자 프로필 Repository
 */
@Repository
class UserProfileRepository(
    private val dsl: DSLContext
) {
    fun save(profile: UserProfile): UserProfile {
        val record = dsl.insertInto(USER_PROFILES)
            .set(USER_PROFILES.USER_ID, profile.userId.toString())
            .set(USER_PROFILES.NICKNAME, profile.nickname)
            .set(USER_PROFILES.PROFILE_IMAGE_URL, profile.profileImageUrl)
            .set(USER_PROFILES.BIO, profile.bio)
            .returning()
            .fetchOne()!!

        return profile.copy(id = record.id)
    }

    fun findByUserId(userId: UUID): UserProfile? {
        return dsl.selectFrom(USER_PROFILES)
            .where(USER_PROFILES.USER_ID.eq(userId.toString()))
            .and(USER_PROFILES.DELETED_AT.isNull)  // Soft delete 필터링
            .fetchOne()
            ?.let { mapToUserProfile(it) }
    }

    fun findByNickname(nickname: String): UserProfile? {
        return dsl.selectFrom(USER_PROFILES)
            .where(USER_PROFILES.NICKNAME.eq(nickname))
            .and(USER_PROFILES.DELETED_AT.isNull)  // Soft delete 필터링
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
            .where(USER_PROFILES.USER_ID.eq(profile.userId.toString()))
            .and(USER_PROFILES.DELETED_AT.isNull)  // Soft delete 필터링
            .execute()

        return profile
    }

    fun existsByNickname(nickname: String): Boolean {
        return dsl.fetchExists(
            dsl.selectFrom(USER_PROFILES)
                .where(USER_PROFILES.NICKNAME.eq(nickname))
                .and(USER_PROFILES.DELETED_AT.isNull)  // Soft delete 필터링
        )
    }

    /**
     * 사용자 ID로 프로필 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @return 프로필 존재 여부
     */
    fun existsByUserId(userId: UUID): Boolean {
        return dsl.fetchExists(
            dsl.selectFrom(USER_PROFILES)
                .where(USER_PROFILES.USER_ID.eq(userId.toString()))
                .and(USER_PROFILES.DELETED_AT.isNull)  // Soft delete 필터링
        )
    }

    /**
     * 프로필 Soft Delete
     *
     * @param userId 사용자 ID
     * @param deletedBy 삭제한 사용자 ID
     */
    fun softDelete(userId: UUID, deletedBy: UUID) {
        dsl.update(USER_PROFILES)
            .set(USER_PROFILES.DELETED_AT, java.time.LocalDateTime.now())
            .set(USER_PROFILES.UPDATED_AT, java.time.LocalDateTime.now())
            .set(USER_PROFILES.UPDATED_BY, deletedBy.toString())
            .where(USER_PROFILES.USER_ID.eq(userId.toString()))
            .and(USER_PROFILES.DELETED_AT.isNull)
            .execute()
    }

    private fun mapToUserProfile(record: UserProfilesRecord): UserProfile {
        return UserProfile(
            id = record.id,
            userId = UUID.fromString(record.userId!!),
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
