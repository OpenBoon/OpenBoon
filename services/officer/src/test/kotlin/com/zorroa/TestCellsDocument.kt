package com.zorroa

import com.aspose.cells.Workbook
import com.aspose.cells.Worksheet
import org.apache.commons.io.FileUtils
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import java.awt.image.BufferedImage
import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestCellsDocument {

    private lateinit var opts: Options

    @Rule
    @JvmField
    var expectedException = ExpectedException.none()!!

    @Before
    fun setup() {
        opts = Options("src/test/resources/test_sheet.xlsx")
        opts.page = 1
        opts.outputDir = "xlsx"
        FileUtils.deleteDirectory(Paths.get(ServerOptions.storagePath).toFile())
    }

    @Ignore
    @Test
    fun testRenderMetadata() {
        val doc = CellsDocument(opts)
        doc.render()

        val metadata = Json.mapper.readValue(doc.getMetadataFile(), Map::class.java)

        assertEquals(3, metadata["pages"])
        assertFalse(metadata.containsKey("content"))
        assertEquals("0001-01-03T05:00:00.000+0000", metadata["timeCreated"])
        assertEquals("0001-01-03T05:00:00.000+0000", metadata["timeModified"])
    }

    @Test
    fun testRenderProxyFromGCS() {
        val opts = Options("gs://zorroa-dev-data/office/Data_Listing-Caswell.xlsx")
        opts.page = 1
        val doc = CellsDocument(opts)
        doc.renderImage()

        val image = ImageIO.read(doc.getImageFile())
        assertEquals(3464, image.width)
        assertEquals(1260, image.height)
    }

    @Test
    fun testRenderProxy() {
        val doc = CellsDocument(opts)
        doc.render()

        val image = ImageIO.read(doc.getImageFile())
        assertEquals(2674, image.width)
        assertEquals(1050, image.height)
    }

    @Test
    fun testRenderAllImages() {
        val opts = Options("src/test/resources/test_sheet.xlsx")
        opts.page = -1

        val doc = CellsDocument(opts)
        doc.renderAllImages()

        val root = doc.getOutputRoot()
        val files = root.toFile().listFiles().toSet().map { it.name }
        assertEquals(3, files.size)
        for (page in 1..3) {
            assertTrue("proxy.1.jpg" in files)
        }
    }

    @Test
    fun testRenderPageMetadata_ContainsNoContent() {
        opts.content = true
        opts.page = 1
        val doc = CellsDocument(opts)
        doc.render()

        val metadata = Json.mapper.readValue(doc.getMetadataFile(), Map::class.java)
        assertFalse(metadata.containsKey("content"))
    }

    @Test
    fun testRenderPageProxy() {
        opts.page = 2
        val doc = CellsDocument(opts)
        doc.render()

        val image = ImageIO.read(doc.getImageFile())
        assertEquals(1346, image.width)
        assertEquals(666, image.height)
    }

    @Test
    fun testRenderLargeSheet_ThrowsException() {
        val doc = CellsDocument(opts)
        expectedException.expect(OutOfMemoryError::class.java)
        doc.saveSheetProxy(largeSheet(), opts.page)
    }

    @Test
    fun testRenderLargeSheetWithCellRange_CreatesProxy() {
        val doc = CellsDocument(opts)
        doc.saveSheetProxyWithCellRange(largeSheet(), opts.page)

        assertThat(
            ImageIO.read(doc.getImageFile()),
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
