package me.onetwo.growsnap.domain.interaction.service

import me.onetwo.growsnap.domain.analytics.repository.ContentInteractionRepository
import me.onetwo.growsnap.domain.interaction.dto.ShareLinkResponse
import me.onetwo.growsnap.domain.interaction.dto.ShareResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class ShareServiceImpl(
    private val contentInteractionRepository: ContentInteractionRepository
) : ShareService {

    override fun shareContent(contentId: UUID): Mono<ShareResponse> {
        logger.debug("Sharing content: contentId={}", contentId)

        return contentInteractionRepository.incrementShareCount(contentId)
            .then(Mono.defer {
                Mono.just(
                    ShareResponse(
                        contentId = contentId.toString(),
                        shareCount = 0
                    )
                )
            })
            .doOnSuccess { logger.debug("Content shared: contentId={}", contentId) }
    }

    override fun getShareLink(contentId: UUID): Mono<ShareLinkResponse> {
        logger.debug("Getting share link: contentId={}", contentId)

        val shareUrl = "https://growsnap.com/watch/$contentId"

        return Mono.just(
            ShareLinkResponse(
                contentId = contentId.toString(),
                shareUrl = shareUrl
            )
        ).doOnSuccess { logger.debug("Share link generated: url={}", shareUrl) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ShareServiceImpl::class.java)
    }
}
