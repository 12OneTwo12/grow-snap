package me.onetwo.growsnap.domain.interaction.controller

import me.onetwo.growsnap.domain.interaction.dto.SaveResponse
import me.onetwo.growsnap.domain.interaction.dto.SavedContentResponse
import me.onetwo.growsnap.domain.interaction.service.SaveService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class SaveController(
    private val saveService: SaveService
) {

    @PostMapping("/videos/{videoId}/save")
    fun saveVideo(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable videoId: String
    ): Mono<ResponseEntity<SaveResponse>> {
        val contentId = UUID.fromString(videoId)

        return saveService.saveContent(userId, contentId)
            .map { response -> ResponseEntity.ok(response) }
    }

    @DeleteMapping("/videos/{videoId}/save")
    fun unsaveVideo(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable videoId: String
    ): Mono<ResponseEntity<SaveResponse>> {
        val contentId = UUID.fromString(videoId)

        return saveService.unsaveContent(userId, contentId)
            .map { response -> ResponseEntity.ok(response) }
    }

    @GetMapping("/users/me/saved-videos")
    fun getSavedVideos(
        @AuthenticationPrincipal userId: UUID
    ): Flux<SavedContentResponse> {
        return saveService.getSavedContents(userId)
    }
}
