package me.onetwo.growsnap.infrastructure.security.jwt

import me.onetwo.growsnap.domain.user.model.UserRole
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.UUID

/**
 * JWT 기반 인증 토큰
 *
 * Spring Security의 Authentication 인터페이스를 구현하여
 * JWT에서 추출한 사용자 정보를 SecurityContext에 저장합니다.
 *
 * @property userId 사용자 ID
 * @property email 사용자 이메일
 * @property role 사용자 역할
 */
class JwtAuthenticationToken(
    private val userId: UUID,
    private val email: String,
    private val role: UserRole
) : Authentication {

    private var authenticated: Boolean = true

    /**
     * 사용자 권한 목록
     *
     * UserRole을 Spring Security의 GrantedAuthority로 변환합니다.
     *
     * @return GrantedAuthority 목록
     */
    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(SimpleGrantedAuthority("ROLE_${role.name}"))
    }

    /**
     * 자격 증명 (Credentials)
     *
     * JWT 인증에서는 사용하지 않으므로 null 반환
     *
     * @return null
     */
    override fun getCredentials(): Any? = null

    /**
     * 사용자 상세 정보 (Details)
     *
     * @return 사용자 이메일
     */
    override fun getDetails(): Any = email

    /**
     * Principal (인증 주체)
     *
     * @return 사용자 ID
     */
    override fun getPrincipal(): Any = userId

    /**
     * 인증 여부
     *
     * @return 인증 여부
     */
    override fun isAuthenticated(): Boolean = authenticated

    /**
     * 인증 여부 설정
     *
     * @param isAuthenticated 인증 여부
     */
    override fun setAuthenticated(isAuthenticated: Boolean) {
        this.authenticated = isAuthenticated
    }

    /**
     * 사용자 이름
     *
     * @return 사용자 이메일
     */
    override fun getName(): String = email

    /**
     * 사용자 ID 가져오기
     *
     * @return 사용자 ID
     */
    fun getUserId(): UUID = userId

    /**
     * 사용자 역할 가져오기
     *
     * @return 사용자 역할
     */
    fun getRole(): UserRole = role
}
