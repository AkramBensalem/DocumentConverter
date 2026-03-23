package me.akram.bensalem.documentconverter.data.response

import kotlinx.serialization.Serializable

@Serializable
data class SignedUrlResponse(val url: String)