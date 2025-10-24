package me.onetwo.growsnap.domain.content.repository

import me.onetwo.growsnap.jooq.generated.tables.ContentMetadata.Companion.CONTENT_METADATA
import me.onetwo.growsnap.jooq.generated.tables.Contents.Companion.CONTENTS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * 콘텐츠 메타데이터 Repository
 *
 * 콘텐츠와 관련된 메타데이터를 조회합니다.
 *
 * @property dsl JOOQ DSL Context
 */
@Repository
class ContentMetadataRepository(
    private val dsl: DSLContext
) {

    /**
     * 여러 콘텐츠의 메타데이터를 일괄 조회
     *
     * N+1 쿼리 문제를 방지하기 위해 IN 절을 사용하여 한 번에 조회합니다.
     *
     * @param contentIds 조회할 콘텐츠 ID 목록
     * @return 콘텐츠 ID를 키로 하는 (제목, 썸네일 URL) Map
     */
    fun findContentInfosByContentIds(contentIds: Set<UUID>): Map<UUID, Pair<String, String>> {
        if (contentIds.isEmpty()) {
            return emptyMap()
        }

        return dsl
            .select(
                CONTENTS.ID,
                CONTENT_METADATA.TITLE,
                CONTENTS.THUMBNAIL_URL
            )
            .from(CONTENTS)
            .join(CONTENT_METADATA).on(CONTENT_METADATA.CONTENT_ID.eq(CONTENTS.ID))
            .where(CONTENTS.ID.`in`(contentIds.map { it.toString() }))
            .and(CONTENTS.DELETED_AT.isNull)
            .fetch()
            .associate {
                UUID.fromString(it.getValue(CONTENTS.ID)) to
                    Pair(
                        it.getValue(CONTENT_METADATA.TITLE) ?: "",
                        it.getValue(CONTENTS.THUMBNAIL_URL) ?: ""
                    )
            }
    }
}
