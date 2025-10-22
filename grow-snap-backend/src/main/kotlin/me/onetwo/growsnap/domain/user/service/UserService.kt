package me.onetwo.growsnap.domain.user.service

import me.onetwo.growsnap.domain.user.model.OAuthProvider
import me.onetwo.growsnap.domain.user.model.User
import java.util.UUID

/**
 * 사용자 관리 서비스 인터페이스
 *
 * 사용자 인증, 등록, 조회 등의 비즈니스 로직을 정의합니다.
 */
interface UserService {

    /**
     * OAuth 제공자와 Provider ID로 사용자 조회 또는 생성
     *
     * OAuth 로그인 시 사용하며, 기존 사용자가 있으면 반환하고 없으면 새로 생성합니다.
     *
     * @param email 사용자 이메일
     * @param provider OAuth 제공자 (GOOGLE, NAVER, KAKAO 등)
     * @param providerId OAuth 제공자에서 제공한 사용자 고유 ID
     * @return 조회되거나 생성된 사용자
     */
    fun findOrCreateOAuthUser(
        email: String,
        provider: OAuthProvider,
        providerId: String
    ): User

    /**
     * 사용자 ID로 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 정보
     * @throws me.onetwo.growsnap.domain.user.exception.UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    fun getUserById(userId: UUID): User

    /**
     * 이메일로 사용자 조회
     *
     * @param email 이메일
     * @return 사용자 정보
     * @throws me.onetwo.growsnap.domain.user.exception.UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    fun getUserByEmail(email: String): User

    /**
     * 사용자 이메일 중복 확인
     *
     * @param email 확인할 이메일
     * @return 중복 여부 (true: 중복, false: 사용 가능)
     */
    fun isEmailDuplicated(email: String): Boolean
}
