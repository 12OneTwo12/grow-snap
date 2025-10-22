package me.onetwo.growsnap.infrastructure.security.oauth2

import me.onetwo.growsnap.domain.user.model.OAuthProvider
import me.onetwo.growsnap.domain.user.model.User
import me.onetwo.growsnap.domain.user.service.UserService
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * OAuth2 사용자 정보를 처리하는 커스텀 서비스
 *
 * Spring Security OAuth2가 제공하는 사용자 정보를 받아서
 * 데이터베이스에 사용자를 생성하거나 조회합니다.
 *
 * @property userService 사용자 서비스
 */
@Service
class CustomReactiveOAuth2UserService(
    private val userService: UserService
) : ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private val delegate = DefaultReactiveOAuth2UserService()

    /**
     * OAuth2 사용자 정보를 로드하고 처리
     *
     * 1. OAuth2 제공자로부터 사용자 정보 조회
     * 2. 사용자가 DB에 있으면 조회, 없으면 신규 생성
     * 3. OAuth2User 객체 반환
     *
     * @param userRequest OAuth2 사용자 요청
     * @return Mono<OAuth2User> OAuth2 사용자 정보
     */
    override fun loadUser(userRequest: OAuth2UserRequest): Mono<OAuth2User> {
        return delegate.loadUser(userRequest)
            .flatMap { oauth2User ->
                val registrationId = userRequest.clientRegistration.registrationId
                val provider = when (registrationId.uppercase()) {
                    "GOOGLE" -> OAuthProvider.GOOGLE
                    "NAVER" -> OAuthProvider.NAVER
                    "KAKAO" -> OAuthProvider.KAKAO
                    else -> throw IllegalArgumentException("지원하지 않는 OAuth 제공자: $registrationId")
                }

                val attributes = oauth2User.attributes
                val userInfo = extractUserInfo(provider, attributes)

                // 사용자 조회 또는 생성
                Mono.fromCallable {
                    userService.findOrCreateOAuthUser(
                        email = userInfo.email,
                        provider = provider,
                        providerId = userInfo.providerId
                    )
                }.map { user ->
                    // OAuth2User에 사용자 ID, 이메일, 역할 정보 추가
                    CustomOAuth2User(
                        oauth2User = oauth2User,
                        userId = user.id!!,
                        email = user.email,
                        role = user.role
                    )
                }
            }
    }

    /**
     * OAuth2 제공자별 사용자 정보 추출
     *
     * @param provider OAuth2 제공자
     * @param attributes OAuth2 속성
     * @return OAuth2UserInfo 사용자 정보
     */
    private fun extractUserInfo(
        provider: OAuthProvider,
        attributes: Map<String, Any>
    ): OAuth2UserInfo {
        return when (provider) {
            OAuthProvider.GOOGLE -> OAuth2UserInfo(
                email = attributes["email"] as String,
                providerId = attributes["sub"] as String,
                name = attributes["name"] as? String,
                profileImageUrl = attributes["picture"] as? String
            )

            OAuthProvider.NAVER -> {
                val response = attributes["response"] as Map<*, *>
                OAuth2UserInfo(
                    email = response["email"] as String,
                    providerId = response["id"] as String,
                    name = response["name"] as? String,
                    profileImageUrl = response["profile_image"] as? String
                )
            }

            OAuthProvider.KAKAO -> {
                val kakaoAccount = attributes["kakao_account"] as Map<*, *>
                val profile = kakaoAccount["profile"] as? Map<*, *>
                OAuth2UserInfo(
                    email = kakaoAccount["email"] as String,
                    providerId = attributes["id"].toString(),
                    name = profile?.get("nickname") as? String,
                    profileImageUrl = profile?.get("profile_image_url") as? String
                )
            }
        }
    }
}

/**
 * OAuth2 사용자 정보 데이터 클래스
 *
 * @property email 이메일
 * @property providerId OAuth2 제공자 ID
 * @property name 이름 (선택)
 * @property profileImageUrl 프로필 이미지 URL (선택)
 */
private data class OAuth2UserInfo(
    val email: String,
    val providerId: String,
    val name: String?,
    val profileImageUrl: String?
)
