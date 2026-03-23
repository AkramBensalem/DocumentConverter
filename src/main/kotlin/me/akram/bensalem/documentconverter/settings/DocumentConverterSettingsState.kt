package me.akram.bensalem.documentconverter.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(name = "DocumentConverterSettingsState", storages = [Storage("document_converter_settings.xml")])
class DocumentConverterSettingsState : PersistentStateComponent<DocumentConverterSettingsState.State> {

    data class State(
        var hasKey: Boolean = false,
        var includeImages: Boolean = true,
        var combinePages: Boolean = true,
        var openAfterConvert: Boolean = true,
        var overwritePolicy: OverwritePolicy = OverwritePolicy.WithSuffix,
        var outputMarkdown: Boolean = true,
        var outputJson: Boolean = true,
        var mode: OcrMode = OcrMode.Offline,
        var markitdownCmd: String = "markitdown",
    )

    enum class OverwritePolicy(
        val displayName: String
    ) {
        Overwrite("Overwrite"),
        SkipExisting("Skip Existing"),
        WithSuffix("With Suffix");

        companion object{
            val options: Array<String>
                get() = entries.map { it.displayName }.toTypedArray()
        }

    }

    enum class OcrMode {
        Offline,
        Mistral
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    var apiKey: String
        get() {
            val attr = credentialAttributes()
            val credentials = PasswordSafe.instance.get(attr)
            return credentials?.getPasswordAsString().orEmpty()
        }
        set(value) {
            val attr = credentialAttributes()
            if (value.isNotEmpty()) {
                PasswordSafe.instance.set(attr, Credentials("document.converter", value))
                state.hasKey = true
            } else {
                PasswordSafe.instance.set(attr, null)
                state.hasKey = false
            }
        }

    fun hasApiKey(): Boolean = state.hasKey

    private fun credentialAttributes(): CredentialAttributes =
        CredentialAttributes("document.converter.apiKey")

    companion object {
        @JvmStatic
        fun getInstance(): DocumentConverterSettingsState = service()
    }
}