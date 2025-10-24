package me.onetwo.growsnap.domain.interaction.repository

import me.onetwo.growsnap.domain.interaction.model.UserSave
import java.util.UUID

/**
 * 사용자 저장 레포지토리 인터페이스
 *
 * 사용자의 콘텐츠 저장 상태를 관리합니다.
 * JOOQ를 사용하여 데이터베이스 CRUD 작업을 수행합니다.
 * Reactive 변환은 Service 계층에서 처리합니다.
 */
interface UserSaveRepository {

    /**
     * 콘텐츠 저장
     *
     * @param userId 사용자 ID
     * @param contentId 콘텐츠 ID
     * @return 생성된 저장 (실패 시 null)
     */
    fun save(userId: UUID, contentId: UUID): UserSave?

    /**
     * 콘텐츠 저장 취소 (Soft Delete)
     *
     * @param userId 사용자 ID
     * @param contentId 콘텐츠 ID
     */
    fun delete(userId: UUID, contentId: UUID)

    /**
     * 저장 여부 확인
     *
     * @param userId 사용자 ID
     * @param contentId 콘텐츠 ID
     * @return 저장 여부 (true: 저장됨, false: 저장 안 됨)
     */
    fun exists(userId: UUID, contentId: UUID): Boolean

    /**
     * 사용자의 저장 목록 조회
     *
     * @param userId 사용자 ID
     * @return 저장 목록
     */
    fun findByUserId(userId: UUID): List<UserSave>
}
