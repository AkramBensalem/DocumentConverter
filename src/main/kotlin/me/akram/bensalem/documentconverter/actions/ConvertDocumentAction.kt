package me.akram.bensalem.documentconverter.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.application.ApplicationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.akram.bensalem.documentconverter.data.Options
import me.akram.bensalem.documentconverter.service.DocumentConverterService
import me.akram.bensalem.documentconverter.settings.DocumentConverterSettingsState
import me.akram.bensalem.documentconverter.ui.ConvertDocumentDialog
import me.akram.bensalem.documentconverter.util.IoUtil
import me.akram.bensalem.documentconverter.util.Notifications
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class ConvertDocumentAction : AnAction() {
    private val log = Logger.getInstance(ConvertDocumentAction::class.java)

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: run {
            e.presentation.isEnabledAndVisible = false
            return
        }
        val show = files.any { it.isDirectory || it.extension.equals("pdf", true) }
        e.presentation.isEnabledAndVisible = show
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val selectedPaths = vFiles.mapNotNull { vf ->
            val io = vf.toNioPathOrNull()
            io
        }
        val documents = IoUtil.listDocumentsRecursively(selectedPaths)
        if (documents.isEmpty()) {
            Notifications.warn(project, "Document Converter", "No documents found in the selection.")
            return
        }

        val settings = DocumentConverterSettingsState.getInstance()

        // Early validation for API key only if Mistral mode is selected
        if (settings.state.mode == DocumentConverterSettingsState.OcrMode.Mistral && (!settings.hasApiKey() || settings.apiKey.isBlank())) {
            Notifications.error(project, "Document Converter", "API key is not configured. Open Settings/Preferences → Tools → Document Converter and set your API key.")
            return
        }

        val dialog = ConvertDocumentDialog(project, documents.size, settings)
        if (!dialog.showAndGet()) return

        // Use the selected values from the dialog
        val overwritePolicy = dialog.selectedOverwritePolicy
        val outputMarkdown = dialog.outputMarkdown
        val outputJson = dialog.outputJson

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Converting documents", true) {
            override fun run(indicator: ProgressIndicator) {
                val service = DocumentConverterService.getInstance(e.project ?: return)
                var successCount = 0
                var failCount = 0
                var skippedCount = 0
                val toRefresh = mutableListOf<File>()
                var firstMd: Path? = null
                var firstError: String? = null
                documents.forEachIndexed { idx, document ->
                    indicator.checkCanceled()
                    indicator.text = "Converting ${document.fileName} (${idx + 1}/${documents.size})"
                    indicator.text2 = "Uploading and processing — this may take a while for large documents…"
                    indicator.isIndeterminate = true
                    try {
                        val outDir = IoUtil.computeOutputDir(document)
                        val options = Options(
                            includeImages = settings.state.includeImages,
                            combinePages = settings.state.combinePages,
                            overwritePolicy = overwritePolicy,
                            mode = settings.state.mode,
                            apiKey = settings.apiKey,
                            outputMarkdown = outputMarkdown,
                            outputJson = outputJson
                        )
                        val result = runBlocking {
                            withContext(Dispatchers.IO) {
                                service.convertDocument(document, outDir, options)
                            }
                        }
                        if (result.error == null && result.createdFiles.isNotEmpty()) {
                            successCount++
                            if (firstMd == null) firstMd = result.markdownFile ?: result.jsonFile
                            result.createdFiles.forEach { toRefresh.add(it.toFile()) }

                            // Move the original file into the output directory
                            try {
                                val targetPdf = outDir.resolve(document.fileName)
                                java.nio.file.Files.move(document, targetPdf, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                                toRefresh.add(targetPdf.toFile())
                                toRefresh.add(document.parent.toFile()) // Refresh source directory
                            } catch (ex: Exception) {
                                log.warn("Failed to move $document to $outDir", ex)
                            }
                        } else if (result.error == null) {
                            skippedCount++

                            if (!document.parent.equals(outDir)) {
                                try {
                                    val targetPdf = outDir.resolve(document.fileName)
                                    java.nio.file.Files.move(
                                        document,
                                        targetPdf,
                                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                                    )
                                    toRefresh.add(targetPdf.toFile())
                                    toRefresh.add(document.parent.toFile()) // Refresh source directory
                                } catch (ex: Exception) {
                                    log.warn("Failed to move skipped document $document to $outDir", ex)
                                }
                            }
                        } else {
                            failCount++
                            if (firstError == null) firstError = result.error
                        }
                        // Switch back to determinate progress between files
                        indicator.isIndeterminate = false
                        indicator.fraction = ((idx + 1).toDouble() / documents.size)

                    } catch (ex: Exception) {
                        log.warn("Failed to convert $document", ex)
                        failCount++
                        if (firstError == null) firstError = ex.message
                        indicator.isIndeterminate = false
                        indicator.fraction = ((idx + 1).toDouble() / documents.size)
                    }
                }

                if (toRefresh.isNotEmpty()) {
                    VfsUtil.markDirtyAndRefresh(false, true, true, *toRefresh.toTypedArray())
                }

                if (successCount == 1 && settings.state.openAfterConvert) {
                    val md = firstMd
                    if (md != null) {
                        ApplicationManager.getApplication().invokeLater {
                            if (!project.isDisposed) {
                                val vf = LocalFileSystem.getInstance().findFileByIoFile(md.toFile())
                                if (vf != null) {
                                    FileEditorManager.getInstance(project).openFile(vf, true)
                                }
                            }
                        }
                    }
                }

                var content = "Converted: $successCount, Failed: $failCount, Skipped: $skippedCount"
                if (failCount > 0 && firstError != null) {
                    content += "\nerror: $firstError"
                }
                when {
                    failCount == 0 -> Notifications.info(project, "Document Converter", content)
                    successCount == 0 -> Notifications.error(project, "Document Converter", content)
                    else -> Notifications.warn(project, "Document Converter", content)
                }
            }
        })
    }
}

private fun VirtualFile.toNioPathOrNull(): Path? = try {
    Paths.get(this.path)
} catch (_: Exception) { null }

