package util

import me.akram.bensalem.documentconverter.settings.DocumentConverterSettingsState
import me.akram.bensalem.documentconverter.util.IoUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class IoUtilTest {

    @Test
    fun `nextAvailable adds suffix`() {
        val dir = Files.createTempDirectory("iou")
        val base = dir.resolve("file.md")
        Files.writeString(base, "a")
        val candidate = IoUtil.nextAvailable(base)
        assertTrue(candidate.fileName.toString().startsWith("file ("))
        assertTrue(candidate.fileName.toString().endsWith(").md"))
    }

    @Test
    fun `writeText SkipExisting returns null when exists`() {
        val dir = Files.createTempDirectory("iou")
        val base = dir.resolve("f.md")
        Files.writeString(base, "a")
        val res = IoUtil.writeText(base, "b", DocumentConverterSettingsState.OverwritePolicy.SkipExisting)
        assertEquals(null, res)
        assertEquals("a", Files.readString(base))
    }

    @Test
    fun `updateImagePaths updates links`() {
        val md = "This is an image ![img1.png](img1.png) and another one ![alt text](img2.jpg)."
        val updated = IoUtil.updateImagePaths(md, listOf("img1.png", "img2.jpg"), "figures")
        assertEquals("This is an image ![img1.png](figures/img1.png) and another one ![alt text](figures/img2.jpg).", updated)
    }
}
