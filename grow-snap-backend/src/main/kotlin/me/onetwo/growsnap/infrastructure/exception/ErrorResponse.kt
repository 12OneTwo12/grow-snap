package me.onetwo.growsnap.infrastructure.exception

import java.time.LocalDateTime

/**
 * 에러 응답 DTO
 *
 * 모든 API 에러에 대해 통일된 형식의 응답을 제공합니다.
 *
 * @property timestamp 에러 발생 시각
 * @property status HTTP 상태 코드
 * @property error 에러 타입 (예: "Bad Request", "Not Found")
 * @property message 사용자에게 표시할 에러 메시지
 * @property path 에러가 발생한 API 경로 (선택사항)
 * @property code 애플리케이션 레벨 에러 코드 (선택사항)
 */
data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String? = null,
    val code: String? = null
)
