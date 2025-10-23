package me.onetwo.growsnap.domain.interaction.service

import me.onetwo.growsnap.domain.analytics.dto.InteractionEventRequest
import me.onetwo.growsnap.domain.analytics.dto.InteractionType
import me.onetwo.growsnap.domain.analytics.repository.ContentInteractionRepository
import me.onetwo.growsnap.domain.analytics.service.AnalyticsService
import me.onetwo.growsnap.domain.interaction.dto.SaveResponse
import me.onetwo.growsnap.domain.interaction.dto.SavedContentResponse
import me.onetwo.growsnap.domain.interaction.repository.UserSaveRepository
import me.onetwo.growsnap.jooq.generated.tables.ContentMetadata.Companion.CONTENT_METADATA
import me.onetwo.growsnap.jooq.generated.tables.Contents.Companion.CONTENTS
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class SaveServiceImpl(
    private val userSaveRepository: UserSaveRepository,
    private val analyticsService: AnalyticsService,
    private val contentInteractionRepository: ContentInteractionRepository,
    private val dslContext: DSLContext
) : SaveService {

    override fun saveContent(userId: UUID, contentId: UUID): Mono<SaveResponse> {
        logger.debug("Saving content: userId={}, contentId={}", userId, contentId)

        return userSaveRepository.exists(userId, contentId)
            .flatMap { exists ->
                if (exists) {
                    logger.debug("Content already saved: userId={}, contentId={}", userId, contentId)
                    getSaveResponse(contentId, true)
                } else {
                    userSaveRepository.save(userId, contentId)
                        .then(
                            analyticsService.trackInteractionEvent(
                                userId,
                                InteractionEventRequest(
                                    contentId = contentId,
                                    interactionType = InteractionType.SAVE
                                )
                            )
                        )
                        .then(getSaveResponse(contentId, true))
                }
            }
            .doOnSuccess { logger.debug("Content saved successfully: userId={}, contentId={}", userId, contentId) }
    }

    override fun unsaveContent(userId: UUID, contentId: UUID): Mono<SaveResponse> {
        logger.debug("Unsaving content: userId={}, contentId={}", userId, contentId)

        return userSaveRepository.exists(userId, contentId)
            .flatMap { exists ->
                if (!exists) {
                    logger.debug("Content not saved: userId={}, contentId={}", userId, contentId)
                    getSaveResponse(contentId, false)
                } else {
                    userSaveRepository.delete(userId, contentId)
                        .then(contentInteractionRepository.decrementSaveCount(contentId))
                        .then(getSaveResponse(contentId, false))
                }
            }
            .doOnSuccess { logger.debug("Content unsaved successfully: userId={}, contentId={}", userId, contentId) }
    }

    override fun getSavedContents(userId: UUID): Flux<SavedContentResponse> {
        logger.debug("Getting saved contents: userId={}", userId)

        return userSaveRepository.findByUserId(userId)
            .flatMap { userSave ->
                Mono.fromCallable {
                    dslContext
                        .select(
                            CONTENT_METADATA.TITLE,
                            CONTENTS.THUMBNAIL_URL
                        )
                        .from(CONTENTS)
                        .join(CONTENT_METADATA).on(CONTENT_METADATA.CONTENT_ID.eq(CONTENTS.ID))
                        .where(CONTENTS.ID.eq(userSave.contentId.toString()))
                        .and(CONTENTS.DELETED_AT.isNull)
                        .fetchOne()
                        ?.let {
                            SavedContentResponse(
                                contentId = userSave.contentId.toString(),
                                title = it.getValue(CONTENT_METADATA.TITLE) ?: "",
                                thumbnailUrl = it.getValue(CONTENTS.THUMBNAIL_URL) ?: "",
                                savedAt = userSave.createdAt.toString()
                            )
                        }
                }.flatMap { Mono.justOrEmpty(it) }
            }
    }

    private fun getSaveResponse(contentId: UUID, isSaved: Boolean): Mono<SaveResponse> {
        return contentInteractionRepository.getSaveCount(contentId)
            .map { saveCount ->
                SaveResponse(
                    contentId = contentId.toString(),
                    saveCount = saveCount,
                    isSaved = isSaved
                )
            }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SaveServiceImpl::class.java)
    }
}
