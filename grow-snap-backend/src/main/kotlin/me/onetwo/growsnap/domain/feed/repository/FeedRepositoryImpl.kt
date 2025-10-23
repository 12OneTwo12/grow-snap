package me.onetwo.growsnap.domain.feed.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import me.onetwo.growsnap.domain.content.model.Category
import me.onetwo.growsnap.domain.content.model.ContentType
import me.onetwo.growsnap.domain.feed.dto.CreatorInfoResponse
import me.onetwo.growsnap.domain.feed.dto.FeedItemResponse
import me.onetwo.growsnap.domain.feed.dto.InteractionInfoResponse
import me.onetwo.growsnap.domain.feed.dto.SubtitleInfoResponse
import me.onetwo.growsnap.jooq.generated.tables.references.CONTENTS
import me.onetwo.growsnap.jooq.generated.tables.references.CONTENT_INTERACTIONS
import me.onetwo.growsnap.jooq.generated.tables.references.CONTENT_METADATA
import me.onetwo.growsnap.jooq.generated.tables.references.CONTENT_SUBTITLES
import me.onetwo.growsnap.jooq.generated.tables.references.FOLLOWS
import me.onetwo.growsnap.jooq.generated.tables.references.USERS
import me.onetwo.growsnap.jooq.generated.tables.references.USER_PROFILES
import me.onetwo.growsnap.jooq.generated.tables.references.USER_VIEW_HISTORY
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * 피드 레포지토리 구현체
 *
 * JOOQ를 사용하여 피드 데이터를 조회합니다.
 *
 * @property dslContext JOOQ DSL Context
 * @property objectMapper JSON 파싱용 ObjectMapper
 */
@Repository
class FeedRepositoryImpl(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper
) : FeedRepository {

    /**
     * 메인 피드 조회
     *
     * 추천 알고리즘 기반으로 사용자에게 맞춤화된 피드를 제공합니다.
     * (현재는 최신 콘텐츠 기반, 향후 추천 알고리즘 추가)
     *
     * @param userId 사용자 ID
     * @param cursor 커서 (마지막 조회 콘텐츠 ID, null이면 첫 페이지)
     * @param limit 조회할 항목 수
     * @param excludeContentIds 제외할 콘텐츠 ID 목록
     * @return 피드 아이템 목록
     */
    override fun findMainFeed(
        userId: UUID,
        cursor: UUID?,
        limit: Int,
        excludeContentIds: List<UUID>
    ): Flux<FeedItemResponse> {
        return Mono.fromCallable {
            var query = dslContext
                .select(
                    CONTENTS.asterisk(),
                    CONTENT_METADATA.asterisk(),
                    CONTENT_INTERACTIONS.asterisk(),
                    USERS.ID,
                    USERS.EMAIL,
                    USER_PROFILES.NICKNAME,
                    USER_PROFILES.PROFILE_IMAGE_URL,
                    USER_PROFILES.FOLLOWER_COUNT
                )
                .from(CONTENTS)
                .join(CONTENT_METADATA).on(CONTENT_METADATA.CONTENT_ID.eq(CONTENTS.ID))
                .join(CONTENT_INTERACTIONS).on(CONTENT_INTERACTIONS.CONTENT_ID.eq(CONTENTS.ID))
                .join(USERS).on(USERS.ID.eq(CONTENTS.CREATOR_ID))
                .join(USER_PROFILES).on(USER_PROFILES.USER_ID.eq(USERS.ID))
                .where(CONTENTS.STATUS.eq("PUBLISHED"))
                .and(CONTENTS.DELETED_AT.isNull)
                .and(CONTENT_METADATA.DELETED_AT.isNull)
                .and(USERS.DELETED_AT.isNull)
                .and(USER_PROFILES.DELETED_AT.isNull)

            // 제외할 콘텐츠 필터링
            if (excludeContentIds.isNotEmpty()) {
                query = query.and(CONTENTS.ID.notIn(excludeContentIds.map { it.toString() }))
            }

            // 커서 기반 페이지네이션
            if (cursor != null) {
                query = query.and(CONTENTS.CREATED_AT.lt(
                    dslContext.select(CONTENTS.CREATED_AT)
                        .from(CONTENTS)
                        .where(CONTENTS.ID.eq(cursor.toString()))
                        .asField()
                ))
            }

            val records = query
                .orderBy(CONTENTS.CREATED_AT.desc())
                .limit(limit)
                .fetch()

            // 모든 콘텐츠 ID 추출
            val contentIds = records.map { UUID.fromString(it.get(CONTENTS.ID)) }

            // 자막 배치 조회 (N+1 문제 방지)
            val subtitlesMap = findSubtitlesByContentIds(contentIds)

            // 레코드를 FeedItemResponse로 변환
            records.map { record -> mapRecordToFeedItem(record, subtitlesMap) }
        }
            .flatMapMany { Flux.fromIterable(it) }
    }

    /**
     * 팔로잉 피드 조회
     *
     * 사용자가 팔로우한 크리에이터의 최신 콘텐츠를 제공합니다.
     *
     * @param userId 사용자 ID
     * @param cursor 커서 (마지막 조회 콘텐츠 ID, null이면 첫 페이지)
     * @param limit 조회할 항목 수
     * @return 피드 아이템 목록
     */
    override fun findFollowingFeed(
        userId: UUID,
        cursor: UUID?,
        limit: Int
    ): Flux<FeedItemResponse> {
        return Mono.fromCallable {
            var query = dslContext
                .select(
                    CONTENTS.asterisk(),
                    CONTENT_METADATA.asterisk(),
                    CONTENT_INTERACTIONS.asterisk(),
                    USERS.ID,
                    USERS.EMAIL,
                    USER_PROFILES.NICKNAME,
                    USER_PROFILES.PROFILE_IMAGE_URL,
                    USER_PROFILES.FOLLOWER_COUNT
                )
                .from(CONTENTS)
                .join(CONTENT_METADATA).on(CONTENT_METADATA.CONTENT_ID.eq(CONTENTS.ID))
                .join(CONTENT_INTERACTIONS).on(CONTENT_INTERACTIONS.CONTENT_ID.eq(CONTENTS.ID))
                .join(USERS).on(USERS.ID.eq(CONTENTS.CREATOR_ID))
                .join(USER_PROFILES).on(USER_PROFILES.USER_ID.eq(USERS.ID))
                .join(FOLLOWS).on(FOLLOWS.FOLLOWING_ID.eq(USERS.ID))
                .where(FOLLOWS.FOLLOWER_ID.eq(userId.toString()))
                .and(FOLLOWS.DELETED_AT.isNull)
                .and(CONTENTS.STATUS.eq("PUBLISHED"))
                .and(CONTENTS.DELETED_AT.isNull)
                .and(CONTENT_METADATA.DELETED_AT.isNull)
                .and(USERS.DELETED_AT.isNull)
                .and(USER_PROFILES.DELETED_AT.isNull)

            // 커서 기반 페이지네이션
            if (cursor != null) {
                query = query.and(CONTENTS.CREATED_AT.lt(
                    dslContext.select(CONTENTS.CREATED_AT)
                        .from(CONTENTS)
                        .where(CONTENTS.ID.eq(cursor.toString()))
                        .asField()
                ))
            }

            val records = query
                .orderBy(CONTENTS.CREATED_AT.desc())
                .limit(limit)
                .fetch()

            // 모든 콘텐츠 ID 추출
            val contentIds = records.map { UUID.fromString(it.get(CONTENTS.ID)) }

            // 자막 배치 조회 (N+1 문제 방지)
            val subtitlesMap = findSubtitlesByContentIds(contentIds)

            // 레코드를 FeedItemResponse로 변환
            records.map { record -> mapRecordToFeedItem(record, subtitlesMap) }
        }
            .flatMapMany { Flux.fromIterable(it) }
    }

    /**
     * 최근 본 콘텐츠 ID 목록 조회
     *
     * 중복 콘텐츠 방지를 위해 사용자가 최근 본 콘텐츠 ID 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param limit 조회할 항목 수
     * @return 최근 본 콘텐츠 ID 목록
     */
    override fun findRecentlyViewedContentIds(userId: UUID, limit: Int): Flux<UUID> {
        return Mono.fromCallable {
            dslContext
                .select(USER_VIEW_HISTORY.CONTENT_ID)
                .from(USER_VIEW_HISTORY)
                .where(USER_VIEW_HISTORY.USER_ID.eq(userId.toString()))
                .and(USER_VIEW_HISTORY.DELETED_AT.isNull)
                .orderBy(USER_VIEW_HISTORY.WATCHED_AT.desc())
                .limit(limit)
                .fetch()
                .map { UUID.fromString(it.get(USER_VIEW_HISTORY.CONTENT_ID)) }
        }
            .flatMapMany { Flux.fromIterable(it) }
    }

    /**
     * 데이터베이스 레코드를 FeedItemResponse로 변환
     *
     * @param record JOOQ 레코드
     * @param subtitlesMap 콘텐츠 ID를 키로 하는 자막 정보 맵
     * @return FeedItemResponse
     */
    private fun mapRecordToFeedItem(
        record: org.jooq.Record,
        subtitlesMap: Map<UUID, List<SubtitleInfoResponse>>
    ): FeedItemResponse {
        val contentId = UUID.fromString(record.get(CONTENTS.ID))

        // 태그 파싱
        val tagsJson = record.get(CONTENT_METADATA.TAGS)
        val tags = if (tagsJson != null) {
            objectMapper.readValue<List<String>>(tagsJson.toString())
        } else {
            emptyList()
        }

        // 자막은 미리 조회한 맵에서 가져오기
        val subtitles = subtitlesMap[contentId] ?: emptyList()

        return FeedItemResponse(
            contentId = contentId,
            contentType = ContentType.valueOf(record.get(CONTENTS.CONTENT_TYPE)!!),
            url = record.get(CONTENTS.URL)!!,
            thumbnailUrl = record.get(CONTENTS.THUMBNAIL_URL)!!,
            duration = record.get(CONTENTS.DURATION),
            width = record.get(CONTENTS.WIDTH)!!,
            height = record.get(CONTENTS.HEIGHT)!!,
            title = record.get(CONTENT_METADATA.TITLE)!!,
            description = record.get(CONTENT_METADATA.DESCRIPTION),
            category = Category.valueOf(record.get(CONTENT_METADATA.CATEGORY)!!),
            tags = tags,
            creator = CreatorInfoResponse(
                userId = UUID.fromString(record.get(USERS.ID)!!),
                nickname = record.get(USER_PROFILES.NICKNAME)!!,
                profileImageUrl = record.get(USER_PROFILES.PROFILE_IMAGE_URL),
                followerCount = record.get(USER_PROFILES.FOLLOWER_COUNT)!!
            ),
            interactions = InteractionInfoResponse(
                likeCount = record.get(CONTENT_INTERACTIONS.LIKE_COUNT)!!,
                commentCount = record.get(CONTENT_INTERACTIONS.COMMENT_COUNT)!!,
                saveCount = record.get(CONTENT_INTERACTIONS.SAVE_COUNT)!!,
                shareCount = record.get(CONTENT_INTERACTIONS.SHARE_COUNT)!!,
                viewCount = record.get(CONTENT_INTERACTIONS.VIEW_COUNT)!!
            ),
            subtitles = subtitles
        )
    }

    /**
     * 여러 콘텐츠의 자막 정보를 배치로 조회 (N+1 문제 방지)
     *
     * @param contentIds 콘텐츠 ID 목록
     * @return 콘텐츠 ID를 키로 하는 자막 정보 맵
     */
    private fun findSubtitlesByContentIds(contentIds: List<UUID>): Map<UUID, List<SubtitleInfoResponse>> {
        if (contentIds.isEmpty()) {
            return emptyMap()
        }

        return dslContext
            .select(
                CONTENT_SUBTITLES.CONTENT_ID,
                CONTENT_SUBTITLES.LANGUAGE,
                CONTENT_SUBTITLES.SUBTITLE_URL
            )
            .from(CONTENT_SUBTITLES)
            .where(CONTENT_SUBTITLES.CONTENT_ID.`in`(contentIds.map { it.toString() }))
            .and(CONTENT_SUBTITLES.DELETED_AT.isNull)
            .fetch()
            .groupBy(
                { UUID.fromString(it.get(CONTENT_SUBTITLES.CONTENT_ID)) },
                { record ->
                    SubtitleInfoResponse(
                        language = record.get(CONTENT_SUBTITLES.LANGUAGE)!!,
                        subtitleUrl = record.get(CONTENT_SUBTITLES.SUBTITLE_URL)!!
                    )
                }
            )
    }
}
