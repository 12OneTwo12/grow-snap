package me.onetwo.growsnap.domain.feed.dto

/**
 * 인터랙션 정보 응답 DTO
 *
 * 콘텐츠의 인터랙션 통계 정보입니다.
 *
 * @property likeCount 좋아요 수
 * @property commentCount 댓글 수
 * @property saveCount 저장 수
 * @property shareCount 공유 수
 * @property viewCount 조회수
 */
data class InteractionInfoResponse(
    val likeCount: Int,
    val commentCount: Int,
    val saveCount: Int,
    val shareCount: Int,
    val viewCount: Int
)
