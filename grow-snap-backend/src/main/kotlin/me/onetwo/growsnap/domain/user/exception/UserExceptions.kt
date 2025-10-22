package me.onetwo.growsnap.domain.user.exception

import java.util.UUID

/**
 * 사용자를 찾을 수 없을 때 발생하는 예외
 *
 * @property message 예외 메시지
 */
class UserNotFoundException(
    override val message: String = "사용자를 찾을 수 없습니다."
) : RuntimeException(message)

/**
 * 사용자 프로필을 찾을 수 없을 때 발생하는 예외
 *
 * @property message 예외 메시지
 */
class UserProfileNotFoundException(
    override val message: String = "사용자 프로필을 찾을 수 없습니다."
) : RuntimeException(message)

/**
 * 중복된 닉네임으로 등록 시도 시 발생하는 예외
 *
 * @property nickname 중복된 닉네임
 */
class DuplicateNicknameException(
    val nickname: String
) : RuntimeException("이미 사용 중인 닉네임입니다: $nickname")

/**
 * 중복된 이메일로 등록 시도 시 발생하는 예외
 *
 * @property email 중복된 이메일
 */
class DuplicateEmailException(
    val email: String
) : RuntimeException("이미 사용 중인 이메일입니다: $email")

/**
 * 이미 팔로우 중인 사용자를 팔로우 시도 시 발생하는 예외
 *
 * @property followingId 팔로우 대상 사용자 ID
 */
class AlreadyFollowingException(
    val followingId: UUID
) : RuntimeException("이미 팔로우 중인 사용자입니다: $followingId")

/**
 * 자기 자신을 팔로우 시도 시 발생하는 예외
 */
class CannotFollowSelfException :
    RuntimeException("자기 자신을 팔로우할 수 없습니다.")

/**
 * 팔로우하지 않은 사용자를 언팔로우 시도 시 발생하는 예외
 *
 * @property followingId 언팔로우 대상 사용자 ID
 */
class NotFollowingException(
    val followingId: UUID
) : RuntimeException("팔로우하지 않은 사용자입니다: $followingId")
