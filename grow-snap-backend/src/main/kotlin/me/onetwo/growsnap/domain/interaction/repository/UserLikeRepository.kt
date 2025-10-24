package me.onetwo.growsnap.domain.interaction.repository

import me.onetwo.growsnap.domain.interaction.model.UserLike
import java.util.UUID

/**
 * 사용자 좋아요 레포지토리 인터페이스
 *
 * 사용자의 콘텐츠 좋아요 상태를 관리합니다.
 * JOOQ를 사용하여 데이터베이스 CRUD 작업을 수행합니다.
 * Reactive 변환은 Service 계층에서 처리합니다.
 */
interface UserLikeRepository {

    /**
     * 좋아요 생성
     *
     * @param userId 사용자 ID
     * @param contentId 콘텐츠 ID
     * @return 생성된 좋아요 (실패 시 null)
     */
    fun save(userId: UUID, contentId: UUID): UserLike?

    /**
     * 좋아요 삭제 (Soft Delete)
     *
     * @param userId 사용자 ID
     * @param contentId 콘텐츠 ID
     */
    fun delete(userId: UUID, contentId: UUID)

    /**
     * 좋아요 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @param contentId 콘텐츠 ID
     * @return 좋아요 여부 (true: 좋아요, false: 좋아요 안 함)
     */
    fun exists(userId: UUID, contentId: UUID): Boolean

    /**
     * 사용자의 좋아요 조회
     *
     * @param userId 사용자 ID
     * @param contentId 콘텐츠 ID
     * @return 좋아요 (없으면 null)
     */
    fun findByUserIdAndContentId(userId: UUID, contentId: UUID): UserLike?
}
