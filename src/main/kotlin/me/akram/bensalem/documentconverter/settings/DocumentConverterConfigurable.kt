package me.akram.bensalem.documentconverter.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import kotlinx.coroutines.runBlocking
import me.akram.bensalem.documentconverter.service.DocumentConverterService
import javax.swing.*

class DocumentConverterConfigurable : Configurable {
    private var panel: JPanel? = null
    private lateinit var apiKeyField: JPasswordField
    private lateinit var includeImages: JBCheckBox
    private lateinit var combinePages: JBCheckBox
    private lateinit var openAfter: JBCheckBox
    private lateinit var outputMarkdown: JBCheckBox
    private lateinit var outputJson: JBCheckBox
    private lateinit var overwritePolicyCombo: JComboBox<String>
    private lateinit var testButton: JButton
    private lateinit var offlineRadio: JRadioButton
    private lateinit var mistralRadio: JRadioButton
    private lateinit var markitdownCmdField: JTextField
    private lateinit var checkMarkitdownButton: JButton

    override fun getDisplayName(): String = "Document Converter"

    override fun createComponent(): JComponent {
        if (panel == null) {
            apiKeyField = JPasswordField(30)
            testButton = JButton("Test Connection")
            includeImages = JBCheckBox("Include images")
            combinePages = JBCheckBox("Combine pages into one file")
            openAfter = JBCheckBox("Open generated files")
            outputMarkdown = JBCheckBox("Output Markdown")
            outputJson = JBCheckBox("Output JSON")
            overwritePolicyCombo = ComboBox(DocumentConverterSettingsState.OverwritePolicy.options)
            offlineRadio = JRadioButton("Offline")
            mistralRadio = JRadioButton("Online")
            ButtonGroup().apply {
                add(offlineRadio)
                add(mistralRadio)
            }

            markitdownCmdField = JBTextField(30)
            checkMarkitdownButton = JButton("Check MarkItDown")

            lateinit var offlineCell: Cell<JRadioButton>
            lateinit var mistralCell: Cell<JRadioButton>

            testButton.addActionListener {
                val s = DocumentConverterSettingsState.getInstance()
                val typed = String(apiKeyField.password)
                val key = if (typed == "********") s.apiKey else typed
                if (key.isBlank()) {
                    Messages.showErrorDialog("Please enter API key before testing.", "Document Converter")
                } else {
                    val project = ProjectManager.getInstance().openProjects.firstOrNull()
                    if (project != null) {
                        runBlocking {
                            val result = DocumentConverterService.getInstance(project).testConnection(key)
                            if (!result.ok) {
                                Messages.showErrorDialog(result.message, "Document Converter")
                            } else {
                                Messages.showInfoMessage(result.message, "Document Converter")
                            }
                        }
                    }
                }
            }

            checkMarkitdownButton.addActionListener {
                val cmd = markitdownCmdField.text.trim()
                if (cmd.isEmpty()) {
                    Messages.showErrorDialog("Please enter the MarkItDown command or full path.", "Document Converter")
                } else {
                    val project = ProjectManager.getInstance().openProjects.firstOrNull()
                    if (project != null) {
                        val result = DocumentConverterService.getInstance(project).checkMarkItDown(cmd)
                        if (!result.ok) {
                            Messages.showErrorDialog(result.message, "Document Converter")
                        } else {
                            Messages.showInfoMessage(result.message, "Document Converter")
                        }
                    } else {
                        try {
                            val proc = ProcessBuilder(cmd, "--version").redirectErrorStream(true).start()
                            val out = proc.inputStream.bufferedReader().use { it.readText() }.trim()
                            val exit = proc.waitFor()
                            if (exit == 0) {
                                Messages.showInfoMessage(if (out.isNotBlank()) out else "MarkItDown detected", "Document Converter")
                            } else {
                                Messages.showErrorDialog(out.ifBlank { "Failed to run MarkItDown" }, "Document Converter")
                            }
                        } catch (e: Exception) {
                            Messages.showErrorDialog("Failed to run MarkItDown: ${e.message}", "Document Converter")
                        }
                    }
                }
            }

            panel = panel {
                group("General Settings") {
                    row {
                        cell(includeImages)
                    }
                    row {
                        cell(combinePages)
                    }
                    row {
                        cell(openAfter)
                    }
                }

                group("OCR Mode") {
                    buttonsGroup {
                        row {
                            offlineCell = cell(offlineRadio)
                                .comment("Uses the <b><a href=\"https://github.com/microsoft/markitdown\">MarkItDown</a></b> CLI locally. No internet required.")
                        }
                        row {
                            mistralCell = cell(mistralRadio)
                                .comment("Uses <b><a href=\"https://mistral.ai/\">Mistral AI</a></b> cloud services. Requires an <a href=\"https://console.mistral.ai/api-keys/\">API key</a>.")
                        }
                    }

                    indent {
                        row("MarkItDown command:") {
                            cell(markitdownCmdField).align(AlignX.FILL)
                            cell(checkMarkitdownButton)
                        }.enabledIf(offlineCell.selected)

                        row("API Key:") {
                            cell(apiKeyField).align(AlignX.FILL)
                            cell(testButton)
                        }.enabledIf(mistralCell.selected)
                    }
                }

                group("Output Settings") {
                    row("Overwrite policy:") {
                        cell(overwritePolicyCombo)
                    }
                    row("Output formats:") {
                        cell(outputMarkdown)
                        cell(outputJson)
                    }
                }
            }
        }
        reset()
        return panel as JPanel
    }

    override fun isModified(): Boolean {
        val s = DocumentConverterSettingsState.getInstance()
        val typed = String(apiKeyField.password)
        val keyChanged = when {
            typed == "********" -> false
            typed.isEmpty() -> s.hasApiKey()
            else -> true
        }
        val modeChanged = when (s.state.mode) {
            DocumentConverterSettingsState.OcrMode.Offline -> !offlineRadio.isSelected
            DocumentConverterSettingsState.OcrMode.Mistral -> !mistralRadio.isSelected
        }
        return keyChanged || modeChanged ||
                includeImages.isSelected != s.state.includeImages ||
                combinePages.isSelected != s.state.combinePages ||
                openAfter.isSelected != s.state.openAfterConvert ||
                outputMarkdown.isSelected != s.state.outputMarkdown ||
                outputJson.isSelected != s.state.outputJson ||
                overwritePolicyCombo.selectedIndex != s.state.overwritePolicy.ordinal ||
                markitdownCmdField.text.trim() != s.state.markitdownCmd
    }

    override fun apply() {
        val s = DocumentConverterSettingsState.getInstance()
        val typed = String(apiKeyField.password)
        when {
            typed == "********" -> { /* no change to stored key */ }
            typed.isNotEmpty() -> {
                s.apiKey = typed
            }
            else -> {
                s.apiKey = ""
            }
        }

        // Reflect masked or empty state in the UI field after applying
        apiKeyField.text = if (s.hasApiKey()) "********" else ""

        val newMode = if (mistralRadio.isSelected) DocumentConverterSettingsState.OcrMode.Mistral else DocumentConverterSettingsState.OcrMode.Offline

        s.loadState(
            s.state.copy(
                includeImages = includeImages.isSelected,
                combinePages = combinePages.isSelected,
                openAfterConvert = openAfter.isSelected,
                outputMarkdown = outputMarkdown.isSelected,
                outputJson = outputJson.isSelected,
                overwritePolicy = DocumentConverterSettingsState.OverwritePolicy.entries[overwritePolicyCombo.selectedIndex],
                mode = newMode,
                markitdownCmd = markitdownCmdField.text.trim(),
            )
        )
    }

    override fun reset() {
        val s = DocumentConverterSettingsState.getInstance()
        apiKeyField.text = if (s.hasApiKey()) "********" else ""
        includeImages.isSelected = s.state.includeImages
        combinePages.isSelected = s.state.combinePages
        openAfter.isSelected = s.state.openAfterConvert
        outputMarkdown.isSelected = s.state.outputMarkdown
        outputJson.isSelected = s.state.outputJson
        overwritePolicyCombo.selectedIndex = s.state.overwritePolicy.ordinal

        // Mode selection
        when (s.state.mode) {
            DocumentConverterSettingsState.OcrMode.Offline -> offlineRadio.isSelected = true
            DocumentConverterSettingsState.OcrMode.Mistral -> mistralRadio.isSelected = true
        }

        // MarkItDown command
        markitdownCmdField.text = s.state.markitdownCmd
    }

    override fun disposeUIResources() {
        panel = null
    }
}