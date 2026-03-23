package me.akram.bensalem.documentconverter.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import me.akram.bensalem.documentconverter.settings.DocumentConverterSettingsState
import javax.swing.Action
import javax.swing.JComponent

class ConvertDocumentDialog(
    project: Project,
    private val documentCount: Int,
    private val settings: DocumentConverterSettingsState
) : DialogWrapper(project) {

    private val overwritePolicyCombo = ComboBox(DocumentConverterSettingsState.OverwritePolicy.options)
    private val outputMarkdownCheckbox = JBCheckBox("Output Markdown", settings.state.outputMarkdown)
    private val outputJsonCheckbox = JBCheckBox("Output JSON", settings.state.outputJson)

    var selectedOverwritePolicy: DocumentConverterSettingsState.OverwritePolicy
        get() = DocumentConverterSettingsState.OverwritePolicy.entries[overwritePolicyCombo.selectedIndex]
        private set(value) {
            overwritePolicyCombo.selectedIndex = value.ordinal
        }

    val outputMarkdown: Boolean
        get() = outputMarkdownCheckbox.isSelected

    val outputJson: Boolean
        get() = outputJsonCheckbox.isSelected

    init {
        title = "Convert Document"
        selectedOverwritePolicy = settings.state.overwritePolicy
        init()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                label("Convert $documentCount document${if (documentCount > 1) "s" else ""}")
            }
            row {
                label("Overwrite Policy:")
                cell(overwritePolicyCombo)
            }
            row {
                cell(outputMarkdownCheckbox)
            }
            row {
                cell(outputJsonCheckbox)
            }
        }
    }

    override fun doValidate(): ValidationInfo? {
        if (!outputMarkdownCheckbox.isSelected && !outputJsonCheckbox.isSelected) {
            return ValidationInfo("At least one output format must be selected")
        }
        return null
    }

    override fun getOKAction() = super.getOKAction().apply {
        putValue(Action.NAME, "Convert")
    }
}
