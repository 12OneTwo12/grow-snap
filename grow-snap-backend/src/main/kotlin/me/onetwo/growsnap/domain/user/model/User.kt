package me.onetwo.growsnap.domain.user.model

import java.time.LocalDateTime

data class User(
    val id: Long? = null,
    val email: String,
    val provider: OAuthProvider,
    val providerId: String,
    val role: UserRole,
    val isCreator: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class OAuthProvider {
    GOOGLE,
    NAVER,
    KAKAO
}

enum class UserRole {
    USER,
    CREATOR,
    ADMIN
}
