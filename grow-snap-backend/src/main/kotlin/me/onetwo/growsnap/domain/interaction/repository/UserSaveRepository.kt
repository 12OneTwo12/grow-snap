package me.onetwo.growsnap.domain.interaction.repository

import me.onetwo.growsnap.domain.interaction.model.UserSave
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface UserSaveRepository {
    fun save(userId: UUID, contentId: UUID): Mono<UserSave>
    fun delete(userId: UUID, contentId: UUID): Mono<Void>
    fun exists(userId: UUID, contentId: UUID): Mono<Boolean>
    fun findByUserId(userId: UUID): Flux<UserSave>
}
