package me.onetwo.growsnap.domain.interaction.service

import me.onetwo.growsnap.domain.interaction.dto.ShareLinkResponse
import me.onetwo.growsnap.domain.interaction.dto.ShareResponse
import reactor.core.publisher.Mono
import java.util.UUID

interface ShareService {
    fun shareContent(contentId: UUID): Mono<ShareResponse>
    fun getShareLink(contentId: UUID): Mono<ShareLinkResponse>
}
