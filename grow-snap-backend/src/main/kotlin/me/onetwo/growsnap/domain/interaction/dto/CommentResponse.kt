package me.onetwo.growsnap.domain.interaction.dto

/**
 * 댓글 응답 DTO
 *
 * @property id 댓글 ID
 * @property contentId 콘텐츠 ID
 * @property userId 작성자 ID
 * @property userNickname 작성자 닉네임
 * @property userProfileImageUrl 작성자 프로필 이미지 URL
 * @property content 댓글 내용
 * @property timestampSeconds 타임스탬프
 * @property parentCommentId 부모 댓글 ID
 * @property createdAt 작성 시각
 * @property replies 대댓글 목록
 */
data class CommentResponse(
    val id: String,
    val contentId: String,
    val userId: String,
    val userNickname: String,
    val userProfileImageUrl: String?,
    val content: String,
    val timestampSeconds: Int?,
    val parentCommentId: String?,
    val createdAt: String,
    val replies: List<CommentResponse> = emptyList()
)
