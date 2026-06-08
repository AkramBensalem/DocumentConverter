package me.akram.bensalem.documentconverter.util

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File
import java.nio.file.Path
import kotlin.io.path.outputStream

object PdfUtil {

    fun extractPages(source: Path, target: Path, pages: List<Int>) {
        Loader.loadPDF(source.toFile()).use { document ->
            val outputDocument = PDDocument()
            
            // PDFBox uses 0-based indexing. The requested pages are already 0-based.
            for (pageIndex in pages) {
                if (pageIndex >= 0 && pageIndex < document.numberOfPages) {
                    outputDocument.addPage(document.getPage(pageIndex))
                }
            }
            
            outputDocument.save(target.toFile())
            outputDocument.close()
        }
    }
}
