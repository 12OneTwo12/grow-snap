package me.onetwo.growsnap.domain.user.service

import me.onetwo.growsnap.domain.user.exception.DuplicateNicknameException
import me.onetwo.growsnap.domain.user.exception.UserProfileNotFoundException
import me.onetwo.growsnap.domain.user.model.UserProfile
import me.onetwo.growsnap.domain.user.repository.UserProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 사용자 프로필 관리 서비스 구현체
 *
 * 프로필 생성, 수정, 조회 등의 비즈니스 로직을 처리합니다.
 *
 * @property userProfileRepository 사용자 프로필 Repository
 * @property userService 사용자 서비스 (사용자 존재 여부 확인용)
 */
@Service
@Transactional(readOnly = true)
class UserProfileServiceImpl(
    private val userProfileRepository: UserProfileRepository,
    private val userService: UserService
) : UserProfileService {

    /**
     * 사용자 프로필 생성
     *
     * @param userId 사용자 ID
     * @param nickname 닉네임 (고유해야 함)
     * @param profileImageUrl 프로필 이미지 URL (선택 사항)
     * @param bio 자기소개 (선택 사항)
     * @return 생성된 프로필 정보
     * @throws DuplicateNicknameException 닉네임이 중복된 경우
     */
    @Transactional
    override fun createProfile(
        userId: UUID,
        nickname: String,
        profileImageUrl: String?,
        bio: String?
    ): UserProfile {
        // 사용자 존재 여부 확인
        userService.getUserById(userId)

        // 닉네임 중복 확인
        if (userProfileRepository.existsByNickname(nickname)) {
            throw DuplicateNicknameException(nickname)
        }

        val profile = UserProfile(
            userId = userId,
            nickname = nickname,
            profileImageUrl = profileImageUrl,
            bio = bio
        )

        return userProfileRepository.save(profile)
    }

    /**
     * 사용자 ID로 프로필 조회
     *
     * @param userId 사용자 ID
     * @return 프로필 정보
     * @throws UserProfileNotFoundException 프로필을 찾을 수 없는 경우
     */
    override fun getProfileByUserId(userId: UUID): UserProfile {
        return userProfileRepository.findByUserId(userId)
            ?: throw UserProfileNotFoundException("프로필을 찾을 수 없습니다. User ID: $userId")
    }

    /**
     * 닉네임으로 프로필 조회
     *
     * @param nickname 닉네임
     * @return 프로필 정보
     * @throws UserProfileNotFoundException 프로필을 찾을 수 없는 경우
     */
    override fun getProfileByNickname(nickname: String): UserProfile {
        return userProfileRepository.findByNickname(nickname)
            ?: throw UserProfileNotFoundException("프로필을 찾을 수 없습니다. Nickname: $nickname")
    }

    /**
     * 닉네임 중복 확인
     *
     * @param nickname 확인할 닉네임
     * @return 중복 여부 (true: 중복, false: 사용 가능)
     */
    override fun isNicknameDuplicated(nickname: String): Boolean {
        return userProfileRepository.existsByNickname(nickname)
    }

    /**
     * 프로필 업데이트
     *
     * 닉네임, 프로필 이미지, 자기소개를 업데이트합니다.
     *
     * @param userId 사용자 ID
     * @param nickname 새 닉네임 (선택 사항)
     * @param profileImageUrl 새 프로필 이미지 URL (선택 사항)
     * @param bio 새 자기소개 (선택 사항)
     * @return 업데이트된 프로필 정보
     * @throws UserProfileNotFoundException 프로필을 찾을 수 없는 경우
     * @throws DuplicateNicknameException 닉네임이 중복된 경우
     */
    @Transactional
    override fun updateProfile(
        userId: UUID,
        nickname: String?,
        profileImageUrl: String?,
        bio: String?
    ): UserProfile {
        val currentProfile = getProfileByUserId(userId)

        // 닉네임 변경 시 중복 확인
        nickname?.let {
            if (it != currentProfile.nickname && userProfileRepository.existsByNickname(it)) {
                throw DuplicateNicknameException(it)
            }
        }

        val updatedProfile = currentProfile.copy(
            nickname = nickname ?: currentProfile.nickname,
            profileImageUrl = profileImageUrl ?: currentProfile.profileImageUrl,
            bio = bio ?: currentProfile.bio
        )

        return userProfileRepository.update(updatedProfile)
    }

    /**
     * 팔로워 수 증가
     *
     * @param userId 사용자 ID
     * @return 업데이트된 프로필 정보
     * @throws UserProfileNotFoundException 프로필을 찾을 수 없는 경우
     */
    @Transactional
    override fun incrementFollowerCount(userId: UUID): UserProfile {
        val profile = getProfileByUserId(userId)
        val updatedProfile = profile.copy(followerCount = profile.followerCount + 1)
        return userProfileRepository.update(updatedProfile)
    }

    /**
     * 팔로워 수 감소
     *
     * @param userId 사용자 ID
     * @return 업데이트된 프로필 정보
     * @throws UserProfileNotFoundException 프로필을 찾을 수 없는 경우
     */
    @Transactional
    override fun decrementFollowerCount(userId: UUID): UserProfile {
        val profile = getProfileByUserId(userId)
        val updatedProfile = profile.copy(
            followerCount = maxOf(0, profile.followerCount - 1)
        )
        return userProfileRepository.update(updatedProfile)
    }

    /**
     * 팔로잉 수 증가
     *
     * @param userId 사용자 ID
     * @return 업데이트된 프로필 정보
     * @throws UserProfileNotFoundException 프로필을 찾을 수 없는 경우
     */
    @Transactional
    override fun incrementFollowingCount(userId: UUID): UserProfile {
        val profile = getProfileByUserId(userId)
        val updatedProfile = profile.copy(followingCount = profile.followingCount + 1)
        return userProfileRepository.update(updatedProfile)
    }

    /**
     * 팔로잉 수 감소
     *
     * @param userId 사용자 ID
     * @return 업데이트된 프로필 정보
     * @throws UserProfileNotFoundException 프로필을 찾을 수 없는 경우
     */
    @Transactional
    override fun decrementFollowingCount(userId: UUID): UserProfile {
        val profile = getProfileByUserId(userId)
        val updatedProfile = profile.copy(
            followingCount = maxOf(0, profile.followingCount - 1)
        )
        return userProfileRepository.update(updatedProfile)
    }
}
