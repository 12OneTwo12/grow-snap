package me.onetwo.growsnap.domain.interaction.controller

import me.onetwo.growsnap.domain.interaction.dto.ShareLinkResponse
import me.onetwo.growsnap.domain.interaction.dto.ShareResponse
import me.onetwo.growsnap.domain.interaction.service.ShareService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api/v1/videos")
class ShareController(
    private val shareService: ShareService
) {

    @PostMapping("/{videoId}/share")
    fun shareVideo(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable videoId: String
    ): Mono<ResponseEntity<ShareResponse>> {
        val contentId = UUID.fromString(videoId)

        return shareService.shareContent(userId, contentId)
            .map { response -> ResponseEntity.ok(response) }
    }

    @GetMapping("/{videoId}/share-link")
    fun getShareLink(
        @PathVariable videoId: String
    ): Mono<ResponseEntity<ShareLinkResponse>> {
        val contentId = UUID.fromString(videoId)

        return shareService.getShareLink(contentId)
            .map { response -> ResponseEntity.ok(response) }
    }
}
