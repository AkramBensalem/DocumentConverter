package service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.akram.bensalem.documentconverter.data.Options
import me.akram.bensalem.documentconverter.service.DocumentConverterService
import me.akram.bensalem.documentconverter.settings.DocumentConverterSettingsState.OcrMode
import me.akram.bensalem.documentconverter.settings.DocumentConverterSettingsState.OverwritePolicy
import me.akram.bensalem.documentconverter.util.IoUtil
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.nio.file.Paths

/**
 * Integration test that verifies large document conversion works end-to-end
 * using the Mistral OCR API (online mode).
 *
 * Prerequisites:
 *  - Set the environment variable MISTRAL_API_KEY to a valid Mistral API key.
 *  - The file scripts/big_book_example.pdf must be present.
 *
 * The test is skipped automatically when either prerequisite is missing,
 * so CI never breaks on machines without credentials.
 */
class MistralOnlineConversionTest {

    @Test
    fun `convert big book pdf online via Mistral without timeout`() {
        val apiKey = System.getenv("MISTRAL_API_KEY").orEmpty()
        assumeTrue(apiKey.isNotBlank(), "MISTRAL_API_KEY env var not set — skipping online Mistral test")

        val pdfPath = Paths.get("scripts/big_book_example.pdf")
        assumeTrue(pdfPath.toFile().exists(), "big_book_example.pdf not found — skipping test")

        val outDir = createTempDir("mistral_online_test_out")
        try {
            val options = Options(
                includeImages = false,
                combinePages = true,
                overwritePolicy = OverwritePolicy.Overwrite,
                mode = OcrMode.Mistral,
                apiKey = apiKey,
                outputMarkdown = true,
                outputJson = false
            )

            val documents = IoUtil.listDocumentsRecursively(listOf(pdfPath))
            assertFalse(documents.isEmpty(), "Should find at least one document")

            val document = documents.first()
            val targetDir = outDir.toPath()

            println("[DEBUG_LOG] Starting Mistral online conversion for: $document")

            val result = runBlocking {
                withContext(Dispatchers.IO) {
                    val service = DocumentConverterService()
                    service.convertDocument(document, targetDir, options)
                }
            }

            println("[DEBUG_LOG] Conversion finished. error=${result.error}, files=${result.createdFiles}")

            assertNull(result.error, "Expected no error but got: ${result.error}")
            assertFalse(result.createdFiles.isEmpty(), "Expected at least one output file")

            val mdFile = result.markdownFile
            assertNotNull(mdFile, "Expected a markdown file to be created")
            val content = mdFile!!.toFile().readText()
            assertTrue(content.isNotBlank(), "Converted markdown should not be empty")
            println("[DEBUG_LOG] Markdown size: ${content.length} chars, first 300:\n${content.take(300)}")
        } finally {
           // outDir.deleteRecursively()
        }
    }

    @Test
    fun `test Mistral API connection`() {
        val apiKey = System.getenv("MISTRAL_API_KEY").orEmpty()
        assumeTrue(apiKey.isNotBlank(), "MISTRAL_API_KEY env var not set — skipping connection test")

        val result = runBlocking {
            withContext(Dispatchers.IO) {
                DocumentConverterService().testConnection(apiKey)
            }
        }

        println("[DEBUG_LOG] Connection test result: success=${result.ok}, message=${result.message}")
        assertTrue(result.ok, "Expected successful connection but got: ${result.message}")
    }
}
