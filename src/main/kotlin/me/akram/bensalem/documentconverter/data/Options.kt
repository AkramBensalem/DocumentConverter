package me.akram.bensalem.documentconverter.data

import me.akram.bensalem.documentconverter.settings.DocumentConverterSettingsState

 data class Options(
     val includeImages: Boolean,
     val combinePages: Boolean,
     val overwritePolicy: DocumentConverterSettingsState.OverwritePolicy,
     val mode: DocumentConverterSettingsState.OcrMode,
     val apiKey: String,
     val outputMarkdown: Boolean,
     val outputJson: Boolean
)