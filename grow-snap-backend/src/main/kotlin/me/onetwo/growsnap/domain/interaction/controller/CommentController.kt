package me.onetwo.growsnap.domain.interaction.controller

import jakarta.validation.Valid
import me.onetwo.growsnap.domain.interaction.dto.CommentRequest
import me.onetwo.growsnap.domain.interaction.dto.CommentResponse
import me.onetwo.growsnap.domain.interaction.service.CommentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class CommentController(
    private val commentService: CommentService
) {

    @PostMapping("/videos/{videoId}/comments")
    fun createComment(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable videoId: String,
        @Valid @RequestBody request: CommentRequest
    ): Mono<ResponseEntity<CommentResponse>> {
        val contentId = UUID.fromString(videoId)

        return commentService.createComment(userId, contentId, request)
            .map { response -> ResponseEntity.status(HttpStatus.CREATED).body(response) }
    }

    @GetMapping("/videos/{videoId}/comments")
    fun getComments(@PathVariable videoId: String): Flux<CommentResponse> {
        val contentId = UUID.fromString(videoId)

        return commentService.getComments(contentId)
    }

    @DeleteMapping("/comments/{commentId}")
    fun deleteComment(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable commentId: String
    ): Mono<ResponseEntity<Void>> {
        val id = UUID.fromString(commentId)
        return commentService.deleteComment(userId, id)
            .then(Mono.just(ResponseEntity.noContent().build<Void>()))
    }
}
