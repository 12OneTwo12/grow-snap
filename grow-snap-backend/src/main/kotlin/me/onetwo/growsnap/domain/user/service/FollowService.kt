package me.onetwo.growsnap.domain.user.service

import me.onetwo.growsnap.domain.user.exception.AlreadyFollowingException
import me.onetwo.growsnap.domain.user.exception.CannotFollowSelfException
import me.onetwo.growsnap.domain.user.exception.NotFollowingException
import me.onetwo.growsnap.domain.user.model.Follow
import me.onetwo.growsnap.domain.user.repository.FollowRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 팔로우 관리 서비스
 *
 * 사용자 간 팔로우/언팔로우 관계를 관리하고, 팔로워/팔로잉 수를 업데이트합니다.
 *
 * @property followRepository 팔로우 Repository
 * @property userService 사용자 서비스 (사용자 존재 여부 확인용)
 * @property userProfileService 사용자 프로필 서비스 (팔로워/팔로잉 수 업데이트용)
 */
@Service
@Transactional(readOnly = true)
class FollowService(
    private val followRepository: FollowRepository,
    private val userService: UserService,
    private val userProfileService: UserProfileService
) {

    /**
     * 사용자 팔로우
     *
     * follower가 following을 팔로우합니다.
     * 팔로우 관계 생성 후 양쪽 사용자의 팔로워/팔로잉 수를 업데이트합니다.
     *
     * @param followerId 팔로우하는 사용자 ID
     * @param followingId 팔로우받는 사용자 ID
     * @return 생성된 팔로우 관계
     * @throws CannotFollowSelfException 자기 자신을 팔로우하려는 경우
     * @throws AlreadyFollowingException 이미 팔로우 중인 경우
     */
    @Transactional
    fun follow(followerId: Long, followingId: Long): Follow {
        // 자기 자신 팔로우 방지
        if (followerId == followingId) {
            throw CannotFollowSelfException()
        }

        // 사용자 존재 여부 확인
        userService.getUserById(followerId)
        userService.getUserById(followingId)

        // 이미 팔로우 중인지 확인
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw AlreadyFollowingException(followingId)
        }

        // 팔로우 관계 생성
        val follow = Follow(
            followerId = followerId,
            followingId = followingId
        )
        val savedFollow = followRepository.save(follow)

        // 팔로워/팔로잉 수 업데이트
        userProfileService.incrementFollowingCount(followerId)
        userProfileService.incrementFollowerCount(followingId)

        return savedFollow
    }

    /**
     * 사용자 언팔로우
     *
     * follower가 following을 언팔로우합니다.
     * 팔로우 관계 삭제 후 양쪽 사용자의 팔로워/팔로잉 수를 업데이트합니다.
     *
     * @param followerId 언팔로우하는 사용자 ID
     * @param followingId 언팔로우받는 사용자 ID
     * @throws NotFollowingException 팔로우하지 않은 사용자를 언팔로우하려는 경우
     */
    @Transactional
    fun unfollow(followerId: Long, followingId: Long) {
        // 사용자 존재 여부 확인
        userService.getUserById(followerId)
        userService.getUserById(followingId)

        // 팔로우 관계 확인
        if (!followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw NotFollowingException(followingId)
        }

        // 팔로우 관계 삭제
        followRepository.delete(followerId, followingId)

        // 팔로워/팔로잉 수 업데이트
        userProfileService.decrementFollowingCount(followerId)
        userProfileService.decrementFollowerCount(followingId)
    }

    /**
     * 팔로우 관계 확인
     *
     * @param followerId 팔로우하는 사용자 ID
     * @param followingId 팔로우받는 사용자 ID
     * @return 팔로우 여부 (true: 팔로우 중, false: 팔로우하지 않음)
     */
    fun isFollowing(followerId: Long, followingId: Long): Boolean {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)
    }

    /**
     * 사용자의 팔로잉 수 조회
     *
     * @param userId 사용자 ID
     * @return 팔로잉 수
     */
    fun getFollowingCount(userId: Long): Int {
        return followRepository.countByFollowerId(userId)
    }

    /**
     * 사용자의 팔로워 수 조회
     *
     * @param userId 사용자 ID
     * @return 팔로워 수
     */
    fun getFollowerCount(userId: Long): Int {
        return followRepository.countByFollowingId(userId)
    }
}
