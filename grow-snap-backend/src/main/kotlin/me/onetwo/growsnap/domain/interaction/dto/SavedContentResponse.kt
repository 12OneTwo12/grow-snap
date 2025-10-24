package me.onetwo.growsnap.domain.interaction.dto

data class SavedContentResponse(
    val contentId: String,
    val title: String,
    val thumbnailUrl: String,
    val savedAt: String
)
