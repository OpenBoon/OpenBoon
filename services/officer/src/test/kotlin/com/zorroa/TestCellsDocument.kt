package com.zorroa

import com.aspose.cells.Workbook
import com.aspose.cells.Worksheet
import java.awt.image.BufferedImage
import java.io.FileInputStream
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class TestCellsDocument {

    private lateinit var opts: RenderRequest

    @Rule
    @JvmField
    var expectedException = ExpectedException.none()!!

    @Before
    fun setup() {
        opts = RenderRequest("src/test/resources/test_sheet.xlsx")
        opts.page = 1
        opts.outputPath = "xlsx"
    }

    @Ignore
    @Test
    fun testRenderMetadata() {
        val doc = CellsDocument(opts, FileInputStream(opts.fileName))
        doc.render()

        val metadata = Json.mapper.readValue(doc.ioHandler.getMetadata(1), Map::class.java)

        assertEquals(3, metadata["pages"])
        assertFalse(metadata.containsKey("content"))
        assertEquals("0001-01-03T05:00:00.000+0000", metadata["timeCreated"])
        assertEquals("0001-01-03T05:00:00.000+0000", metadata["timeModified"])
    }

    @Test
    fun testRenderProxy() {
        val doc = CellsDocument(opts, FileInputStream(opts.fileName))
        doc.render()

        val image = ImageIO.read(doc.ioHandler.getImage())
        assertEquals(2674, image.width)
        assertEquals(1050, image.height)
    }

    @Test
    fun testRenderAllImages() {
        val opts = RenderRequest("src/test/resources/test_sheet.xlsx")

        val doc = CellsDocument(opts, FileInputStream(opts.fileName))
        doc.renderAllImages()

        for (page in 1..3) {
            doc.ioHandler.getImage(page)
        }
    }

    @Test
    fun testRenderPageMetadata() {
        opts.page = 1
        val doc = CellsDocument(opts, FileInputStream(opts.fileName))
        doc.renderMetadata(1)
        validateMetadata(doc.getMetadata(1), "width", "height", "orientation", "content")
    }

    @Test
    fun testRenderPageProxy() {
        opts.page = 2
        val doc = CellsDocument(opts, FileInputStream(opts.fileName))
        doc.render()

        val image = ImageIO.read(doc.getImage(2))
        assertEquals(1346, image.width)
        assertEquals(666, image.height)
    }

    @Test
    fun testRenderLargeSheet_ThrowsException() {
        val doc = CellsDocument(opts, FileInputStream(opts.fileName))
        expectedException.expect(OutOfMemoryError::class.java)
        doc.saveSheetProxy(largeSheet(), opts.page)
    }

    @Test
    fun testRenderLargeSheetWithCellRange_CreatesProxy() {
        val doc = CellsDocument(opts, FileInputStream(opts.fileName))
        doc.saveSheetProxyWithCellRange(largeSheet(), 1)

        assertThat(
            ImageIO.read(doc.getImage(1)),
            instanceOf(BufferedImage::class.java)
        )
    }

    private fun largeSheet(): Worksheet {
        val workbook = Workbook()
        val sheet = workbook.worksheets.get(0)
        val cells = sheet.cells
        var i = 1
        for (row in 0..1000) {
            for (col in 0..1000) {
                cells[row, col].putValue(i++)
            }
        }
        return sheet
    }

    private fun smallSheet(): Worksheet {
        val workbook = Workbook()
        val sheet = workbook.worksheets.get(0)
        val cells = sheet.cells
        cells[1, 1].putValue(1)
        return sheet
    }
}
