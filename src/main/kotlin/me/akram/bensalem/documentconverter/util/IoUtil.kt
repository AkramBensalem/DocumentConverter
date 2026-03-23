package me.akram.bensalem.documentconverter.util

import me.akram.bensalem.documentconverter.settings.DocumentConverterSettingsState
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension

object IoUtil {

    fun listDocumentsRecursively(paths: List<Path>): List<Path> = buildList {
        for (p in paths) {
            if (p.isDirectory()) {
                Files.walk(p).use { stream ->
                    stream.filter { Files.isRegularFile(it) && it.extension.equals("pdf", ignoreCase = true) }
                        .forEach { add(it) }
                }
            } else if (Files.isRegularFile(p) && p.extension.equals("pdf", true)) {
                add(p)
            }
        }
    }

    fun computeOutputDir(
        pdf: Path,
    ): Path {
        val stem = pdf.nameWithoutExtension
        return pdf.parent.resolve(stem)
    }

    fun ensureDir(dir: Path) {
        Files.createDirectories(dir)
    }

    fun base64ToBytes(data: String): ByteArray = Base64.getDecoder().decode(data)

    fun writeBytes(target: Path, bytes: ByteArray, overwrite: DocumentConverterSettingsState.OverwritePolicy): Path? {
        val final = when (overwrite) {
            DocumentConverterSettingsState.OverwritePolicy.Overwrite -> target
            DocumentConverterSettingsState.OverwritePolicy.SkipExisting -> if (target.exists()) null else target
            DocumentConverterSettingsState.OverwritePolicy.WithSuffix -> nextAvailable(target)
        } ?: return null
        
        final.parent?.let { ensureDir(it) }
        Files.write(final, bytes)
        return final
    }

    fun writeText(target: Path, text: String, overwrite: DocumentConverterSettingsState.OverwritePolicy): Path? =
        writeBytes(target, text.toByteArray(Charsets.UTF_8), overwrite)

    fun nextAvailable(target: Path): Path {
        if (!target.exists()) return target
        val parent = target.parent
        val fileName = target.fileName.toString()
        val dot = fileName.lastIndexOf('.')
        val base = if (dot >= 0) fileName.substring(0, dot) else fileName
        val ext = if (dot >= 0) fileName.substring(dot) else ""
        var i = 1
        while (true) {
            val candidate = parent.resolve("$base ($i)$ext")
            if (!Files.exists(candidate)) return candidate
            i++
        }
    }

    /**
     * Updates markdown content to point to images in a subdirectory.
     */
    fun updateImagePaths(markdown: String, imageIds: List<String>, subDir: String): String {
        var result = markdown
        for (id in imageIds) {
            // Mistral OCR references images using ![id](id) or ![some text](id)
            // Replace ](id) with ](subDir/id)
            result = result.replace("]($id)", "]($subDir/$id)")
        }
        return result
    }
}
