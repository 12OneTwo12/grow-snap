package me.onetwo.growsnap.infrastructure.common.dto

/**
 * 커서 기반 페이지네이션 요청 DTO
 *
 * 무한 스크롤을 지원하는 커서 기반 페이지네이션 요청 정보를 담습니다.
 * 오프셋 기반 페이지네이션보다 성능이 우수하며, 실시간 데이터 추가에 안전합니다.
 *
 * @property cursor 이전 페이지의 마지막 항목 ID (null이면 첫 페이지)
 * @property limit 페이지당 항목 수 (기본값: 20, 최대: 100)
 */
data class CursorPageRequest(
    val cursor: String? = null,
    val limit: Int = 20
) {
    init {
        require(limit in 1..100) { "Limit must be between 1 and 100" }
    }

    companion object {
        const val DEFAULT_LIMIT = 20
        const val MAX_LIMIT = 100
    }
}
