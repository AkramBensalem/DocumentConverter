package me.akram.bensalem.documentconverter.data.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OcrRequest(
    val model: String,
    val document: Document,
    @SerialName("include_image_base64") val includeImageBase64: Boolean,
    val pages: List<Int>? = null
) {
    @Serializable
    data class Document(
        val type: String,
        @SerialName("document_url") val documentUrl: String
    )
}