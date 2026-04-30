package util

import me.akram.bensalem.documentconverter.util.PdfUtil
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import org.apache.pdfbox.pdmodel.PDDocument

class PdfUtilTest {

    @Test
    fun `test extract pages`() {
        val source = Paths.get("assets/simple.pdf")
        assertTrue(source.toFile().exists(), "assets/simple.pdf must exist")
        
        val tempDir = Files.createTempDirectory("pdf_util_test")
        val target = tempDir.resolve("extracted.pdf")
        
        // Extract page 1 (index 0)
        PdfUtil.extractPages(source, target, listOf(0))
        
        assertTrue(target.toFile().exists(), "Extracted PDF should exist")
        
        PDDocument.load(target.toFile()).use { document ->
            assertTrue(document.numberOfPages == 1, "Extracted PDF should have 1 page")
        }
    }
}
