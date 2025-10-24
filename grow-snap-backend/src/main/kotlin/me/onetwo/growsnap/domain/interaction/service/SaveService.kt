package me.onetwo.growsnap.domain.interaction.service

import me.onetwo.growsnap.domain.interaction.dto.SaveResponse
import me.onetwo.growsnap.domain.interaction.dto.SavedContentResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface SaveService {
    fun saveContent(userId: UUID, contentId: UUID): Mono<SaveResponse>
    fun unsaveContent(userId: UUID, contentId: UUID): Mono<SaveResponse>
    fun getSavedContents(userId: UUID): Flux<SavedContentResponse>
}
