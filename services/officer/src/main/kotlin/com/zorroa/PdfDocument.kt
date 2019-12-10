package com.zorroa

import com.aspose.pdf.Document
import com.aspose.pdf.DocumentInfo
import com.aspose.pdf.devices.JpegDevice
import com.aspose.pdf.devices.Resolution
import com.aspose.pdf.facades.PdfExtractor
import com.aspose.pdf.facades.PdfFileInfo
import java.awt.Color
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import kotlin.system.measureTimeMillis

/**
 * Handles rendering a PDF as an image and json metadata file.
 */
class PdfDocument(options: Options, inputStream: InputStream) : com.zorroa.Document(options) {

    val pdfDocument = Document(inputStream)

    init {
        logger.info("opening file: {}", options.fileName)
    }

    override fun renderAllImages(): Int {
        val stack = PdfImageRenderStack(this, options)
        val fileInfo = PdfFileInfo(pdfDocument)
        for (page in 1..fileInfo.numberOfPages) {
            stack.renderImage(page)
        }
        stack.renderImage(0)
        return fileInfo.numberOfPages + 1
    }

    override fun renderAllMetadata(): Int {
        val fileInfo = PdfFileInfo(pdfDocument)
        for (page in 1..fileInfo.numberOfPages) {
            renderMetadata(page)
        }
        renderMetadata(0)
        return fileInfo.numberOfPages + 1
    }

    override fun renderImage(page: Int) {
        PdfImageRenderStack(this, options).renderImage(page)
    }

    override fun renderMetadata(page: Int) {
        val time = measureTimeMillis {
            val documentInfo = DocumentInfo(pdfDocument)
            val fileInfo = PdfFileInfo(pdfDocument)
            val metadata = mutableMapOf<String, Any?>()

            if (page == 0) {
                metadata["type"] = "document"
                metadata["title"] = fileInfo.title
                metadata["author"] = fileInfo.author
                metadata["keywords"] = fileInfo.keywords
                metadata["description"] = fileInfo.subject
                metadata["timeCreated"] = convertDate(documentInfo.creationDate)
                metadata["length"] = fileInfo.numberOfPages
            }
            val virtPage = page.coerceAtLeast(1)
            val height = fileInfo.getPageHeight(virtPage)
            val width = fileInfo.getPageWidth(virtPage)

            metadata["height"] = height
            metadata["width"] = width
            metadata["orientation"] = if (height > width) "portrait" else "landscape"

            if (page > 0) {
                metadata["content"] = extractPageContent(page)
            }

            val output = ReversibleByteArrayOutputStream()
            Json.mapper.writeValue(output, metadata)
            ioHandler.writeMetadata(page, output)
        }

        logMetadataTime(page, time)
    }

    private fun extractPageContent(page: Int): String? {
        val pdfExtractor = PdfExtractor()

        pdfExtractor.bindPdf(pdfDocument)
        pdfExtractor.startPage = page
        pdfExtractor.endPage = page
        pdfExtractor.extractText(Charset.forName("UTF-8"))

        val byteStream = ByteArrayOutputStream()
        pdfExtractor.getText(byteStream)

        return byteStream.toString("UTF-8").replace(whitespaceRegex, " ")
    }

    override fun close() {
        try {
            logger.info("closing file: {}", options.fileName)
            pdfDocument.close()
        } catch (e: Exception) {
            // ignore
        }
    }

    companion object {
        init {
            val classLoader = this::class.java.classLoader
            val licenseAsStream = classLoader.getResourceAsStream(ASPOSE_LICENSE_FILE)
            com.aspose.pdf.License().setLicense(licenseAsStream)
        }

        fun extractPdfText(byteArray: ByteArray): String {
            val pdfStream = ByteArrayInputStream(byteArray)

            val tmpPdf = Document(pdfStream)
            val pdfExtractor = PdfExtractor()

            pdfExtractor.bindPdf(tmpPdf)
            val byteStream = ByteArrayOutputStream()

            pdfExtractor.extractText(Charset.forName("UTF-8"))
            pdfExtractor.getText(byteStream)
            return byteStream.toString("UTF-8")
        }
    }

    /**
     * A helper class which makes it easy to resuse a bytestream and
     * jpegDevice when rendering all pages.
     */
    class PdfImageRenderStack(private val doc: PdfDocument, val options: Options) {

        private val byteStream = ReversibleByteArrayOutputStream(IOHandler.IMG_BUFFER_SIZE)
        private val jpegDevice = JpegDevice(Resolution(75), 100)

        fun renderImage(page: Int) {
            val time = measureTimeMillis {
                jpegDevice.process(doc.pdfDocument.pages.get_Item(page.coerceAtLeast(1)), byteStream)
                if (page == 0) {
                    val render = StackRender(
                        "PDF", Color(227, 50, 34),
                        byteStream.toInputStream()
                    )
                    doc.ioHandler.writeImage(page, render.render())
                } else {
                    doc.ioHandler.writeImage(page, byteStream)
                }
                byteStream.reset()
            }
            doc.logImageTime(page, time)
        }
    }
}
