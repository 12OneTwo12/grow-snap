package me.onetwo.growsnap.domain.user.service

import me.onetwo.growsnap.domain.user.exception.UserNotFoundException
import me.onetwo.growsnap.domain.user.model.OAuthProvider
import me.onetwo.growsnap.domain.user.model.User
import me.onetwo.growsnap.domain.user.model.UserProfile
import me.onetwo.growsnap.domain.user.repository.FollowRepository
import me.onetwo.growsnap.domain.user.repository.UserProfileRepository
import me.onetwo.growsnap.domain.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 사용자 관리 서비스 구현체
 *
 * 사용자 인증, 등록, 조회 등의 비즈니스 로직을 처리합니다.
 *
 * @property userRepository 사용자 Repository
 * @property userProfileRepository 사용자 프로필 Repository
 * @property followRepository 팔로우 Repository
 */
@Service
@Transactional(readOnly = true)
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val followRepository: FollowRepository
) : UserService {

    private val logger = LoggerFactory.getLogger(UserServiceImpl::class.java)

    /**
     * OAuth 제공자와 Provider ID로 사용자 조회 또는 생성
     *
     * OAuth 로그인 시 사용하며, 기존 사용자가 있으면 반환하고 없으면 새로 생성합니다.
     * 신규 사용자 생성 시 프로필도 자동으로 생성됩니다.
     *
     * ### 처리 흐름
     * 1. Provider + ProviderId로 기존 사용자 조회
     * 2. 없으면 신규 사용자 생성
     * 3. 신규 사용자인 경우 프로필 자동 생성 (OAuth 이름을 닉네임으로 사용)
     * 4. 닉네임 중복 시 고유 suffix 추가 (예: "John_a1b2c3")
     *
     * @param email 사용자 이메일
     * @param provider OAuth 제공자 (GOOGLE, NAVER, KAKAO 등)
     * @param providerId OAuth 제공자에서 제공한 사용자 고유 ID
     * @param name OAuth 제공자에서 제공한 사용자 이름 (닉네임으로 사용)
     * @param profileImageUrl OAuth 제공자에서 제공한 프로필 이미지 URL
     * @return 조회되거나 생성된 사용자
     */
    @Transactional
    override fun findOrCreateOAuthUser(
        email: String,
        provider: OAuthProvider,
        providerId: String,
        name: String?,
        profileImageUrl: String?
    ): User {
        // 기존 사용자 조회 (Provider + ProviderId로)
        userRepository.findByProviderAndProviderId(provider, providerId)?.let {
            logger.info("Existing user found: userId=${it.id}, email=$email")
            return it
        }

        // 신규 사용자 생성
        val newUser = User(
            email = email,
            provider = provider,
            providerId = providerId
        )
        val savedUser = userRepository.save(newUser)
        logger.info("New user created: userId=${savedUser.id}, email=$email, provider=$provider")

        // 프로필 자동 생성
        val nickname = generateUniqueNickname(name ?: email.substringBefore("@"))
        val profile = UserProfile(
            userId = savedUser.id!!,
            nickname = nickname,
            profileImageUrl = profileImageUrl,
            bio = null
        )
        userProfileRepository.save(profile)
        logger.info("Auto-created profile for user: userId=${savedUser.id}, nickname=$nickname")

        return savedUser
    }

    /**
     * 고유한 닉네임 생성
     *
     * 닉네임이 중복되면 6자리 랜덤 suffix를 추가합니다.
     * 예: "John" -> "John_a1b2c3"
     *
     * @param baseName 기본 닉네임
     * @return 고유한 닉네임
     */
    private fun generateUniqueNickname(baseName: String): String {
        var nickname = baseName.take(20) // 닉네임 최대 20자 제한
        var attempts = 0
        val maxAttempts = 10

        while (userProfileRepository.existsByNickname(nickname) && attempts < maxAttempts) {
            val suffix = UUID.randomUUID().toString().replace("-", "").take(6)
            nickname = "${baseName.take(14)}_$suffix" // suffix 포함 최대 21자
            attempts++
        }

        if (attempts >= maxAttempts) {
            // 만약 10번 시도해도 중복이면 UUID 사용
            nickname = "user_${UUID.randomUUID().toString().replace("-", "").take(10)}"
            logger.warn("Failed to generate unique nickname after $maxAttempts attempts, using UUID: $nickname")
        }

        return nickname
    }

    /**
     * 사용자 ID로 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 정보
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    override fun getUserById(userId: UUID): User {
        return userRepository.findById(userId)
            ?: throw UserNotFoundException("사용자를 찾을 수 없습니다. ID: $userId")
    }

    /**
     * 이메일로 사용자 조회
     *
     * @param email 이메일
     * @return 사용자 정보
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    override fun getUserByEmail(email: String): User {
        return userRepository.findByEmail(email)
            ?: throw UserNotFoundException("사용자를 찾을 수 없습니다. Email: $email")
    }

    /**
     * 사용자 이메일 중복 확인
     *
     * @param email 확인할 이메일
     * @return 중복 여부 (true: 중복, false: 사용 가능)
     */
    override fun isEmailDuplicated(email: String): Boolean {
        return userRepository.findByEmail(email) != null
    }

    /**
     * 사용자 회원 탈퇴 (Soft Delete)
     *
     * 사용자, 프로필, 팔로우 관계를 모두 soft delete 처리합니다.
     *
     * ### 처리 흐름
     * 1. 사용자 존재 여부 확인
     * 2. 사용자 정보 soft delete
     * 3. 프로필 정보 soft delete
     * 4. 팔로우/팔로잉 관계 soft delete (양방향 모두)
     *
     * @param userId 탈퇴할 사용자 ID
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Transactional
    override fun withdrawUser(userId: UUID) {
        // 사용자 존재 여부 확인
        val user = getUserById(userId)
        logger.info("Starting user withdrawal: userId=$userId, email=${user.email}")

        // 1. 사용자 soft delete
        userRepository.softDelete(userId, userId)
        logger.info("User soft deleted: userId=$userId")

        // 2. 프로필 soft delete
        userProfileRepository.softDelete(userId, userId)
        logger.info("User profile soft deleted: userId=$userId")

        // 3. 팔로우 관계 soft delete (양방향 모두)
        followRepository.softDeleteAllByUserId(userId, userId)
        logger.info("All follow relationships soft deleted: userId=$userId")

        logger.info("User withdrawal completed: userId=$userId")
    }
}
