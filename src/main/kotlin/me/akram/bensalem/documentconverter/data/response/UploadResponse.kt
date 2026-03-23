package me.akram.bensalem.documentconverter.data.response

import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(
    val id: String,
    val signature: String
)