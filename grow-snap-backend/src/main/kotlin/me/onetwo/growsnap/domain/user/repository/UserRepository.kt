package me.onetwo.growsnap.domain.user.repository

import me.onetwo.growsnap.jooq.generated.tables.references.USERS
import me.onetwo.growsnap.domain.user.model.OAuthProvider
import me.onetwo.growsnap.domain.user.model.User
import me.onetwo.growsnap.domain.user.model.UserRole
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * 사용자 Repository
 *
 * JOOQ를 사용하여 users 테이블에 접근합니다.
 *
 * @property dsl JOOQ DSL Context
 */
@Repository
class UserRepository(
    private val dsl: DSLContext
) {
    /**
     * 사용자 저장
     *
     * @param user 사용자 정보
     * @return 저장된 사용자 (ID 포함)
     */
    fun save(user: User): User {
        val userId = user.id ?: UUID.randomUUID()
        dsl.insertInto(USERS)
            .set(USERS.ID, userId.toString())
            .set(USERS.EMAIL, user.email)
            .set(USERS.PROVIDER, user.provider.name)
            .set(USERS.PROVIDER_ID, user.providerId)
            .set(USERS.ROLE, user.role.name)
            .execute()

        return user.copy(id = userId)
    }

    /**
     * 이메일로 사용자 조회
     *
     * @param email 이메일
     * @return 사용자 정보 (존재하지 않으면 null)
     */
    fun findByEmail(email: String): User? {
        return dsl.selectFrom(USERS)
            .where(USERS.EMAIL.eq(email))
            .fetchOne()
            ?.let { mapToUser(it) }
    }

    /**
     * Provider와 Provider ID로 사용자 조회
     *
     * @param provider OAuth Provider
     * @param providerId Provider에서 제공한 사용자 ID
     * @return 사용자 정보 (존재하지 않으면 null)
     */
    fun findByProviderAndProviderId(provider: OAuthProvider, providerId: String): User? {
        return dsl.selectFrom(USERS)
            .where(USERS.PROVIDER.eq(provider.name))
            .and(USERS.PROVIDER_ID.eq(providerId))
            .fetchOne()
            ?.let { mapToUser(it) }
    }

    /**
     * ID로 사용자 조회
     *
     * @param id 사용자 ID
     * @return 사용자 정보 (존재하지 않으면 null)
     */
    fun findById(id: UUID): User? {
        return dsl.selectFrom(USERS)
            .where(USERS.ID.eq(id.toString()))
            .fetchOne()
            ?.let { mapToUser(it) }
    }

    /**
     * JOOQ Record를 User 도메인 모델로 변환
     */
    private fun mapToUser(record: me.onetwo.growsnap.jooq.generated.tables.records.UsersRecord): User {
        return User(
            id = UUID.fromString(record.id!!),
            email = record.email!!,
            provider = OAuthProvider.valueOf(record.provider!!),
            providerId = record.providerId!!,
            role = UserRole.valueOf(record.role!!),
            createdAt = record.createdAt!!,
            updatedAt = record.updatedAt!!
        )
    }
}
