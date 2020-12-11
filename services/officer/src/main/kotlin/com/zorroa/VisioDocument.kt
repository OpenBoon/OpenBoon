package com.zorroa

import com.aspose.diagram.CompositingQuality
import com.aspose.diagram.Diagram
import com.aspose.diagram.ImageSaveOptions
import com.aspose.diagram.InterpolationMode
import com.aspose.diagram.PageSize
import com.aspose.diagram.PaperSizeFormat
import com.aspose.diagram.PixelOffsetMode
import com.aspose.diagram.SaveFileFormat
import com.aspose.diagram.SmoothingMode
import java.io.InputStream
import kotlin.system.measureTimeMillis

class VisioDocument(options: RenderRequest, inputStream: InputStream) : Document(options) {

    var diagram = Diagram(inputStream)

    override fun renderImage(page: Int) {
        saveImage(page, getImageSaveOptions(page))
    }

    override fun renderAllImages(): Int {
        val opts = getImageSaveOptions(null)
        for (index in 0 until diagram.pages.count()) {
            opts.pageIndex = index
            saveImage(index + 1, opts)
        }
        // Render the parent page
        opts.pageIndex = 0
        return diagram.pages.count()
    }

    override fun pageCount(): Int {
        return diagram.pages.count()
    }

    fun saveImage(page: Int, opts: ImageSaveOptions) {
        val time = measureTimeMillis {
            val output = ReversibleByteArrayOutputStream(IOHandler.IMG_BUFFER_SIZE)
            diagram.save(output, opts)
            ioHandler.writeImage(page, output)
        }
        logImageTime(page, time)
    }

    override fun renderAllMetadata(): Int {
        for (index in 0 until diagram.pages.count()) {
            renderMetadata(index + 1)
        }
        return diagram.pages.count()
    }

    override fun renderMetadata(page: Int) {
        val time = measureTimeMillis {
            val props = diagram.documentProps
            val metadata = mutableMapOf<String, Any?>()

            metadata["type"] = "document"
            metadata["title"] = props.title
            metadata["author"] = props.creator
            metadata["keywords"] = props.keywords
            metadata["timeCreated"] = convertDate(props.timeCreated?.toDate())
            metadata["length"] = diagram.pages.count
            metadata["pageNumber"] = page

            val dpage = diagram.pages[(page - 1).coerceAtLeast(0)]
            val pageProps = dpage.pageSheet.pageProps

            metadata["height"] = (pageProps.pageHeight.value * 10).toInt()
            metadata["width"] = (pageProps.pageHeight.value * 10).toInt()
            metadata["orientation"] =
                if (pageProps.pageHeight.value > pageProps.pageHeight.value) "portrait" else "landscape"
            metadata["content"] = extractContent(page)
            metadata["description"] = dpage.name

            val output = ReversibleByteArrayOutputStream()
            Json.mapper.writeValue(output, metadata)
            ioHandler.writeMetadata(page, output)
        }

        logMetadataTime(page, time)
    }

    override fun close() {}

    private fun extractContent(page: Int): String {
        val content = StringBuilder(2048)
        val dpage = diagram.pages[page - 1]

        for (i in 0 until dpage.shapes.count) {
            val shape = dpage.shapes[i]
            val text = prepContent(shape.text.value.text)
            content.append(text)
        }
        return content.toString()
    }

    /**
     * Strips HTML type markup and newlines.
     */
    private fun prepContent(string: String?): String? {
        if (string.isNullOrEmpty()) {
            return string
        }
        return string
            .replace(REGEX_STRIP_TAGS, "")
            .replace(REGEX_REPLACE_NR, " ")
    }

    private fun getImageSaveOptions(page: Int?): ImageSaveOptions {
        val opts = ImageSaveOptions(SaveFileFormat.JPEG)
        opts.compositingQuality = CompositingQuality.HIGH_QUALITY
        opts.defaultFont = "MS Gothic"

        val pgSize = PageSize(PaperSizeFormat.A_1)
        opts.pageSize = pgSize
        opts.scale = .5f
        opts.pageCount = 1
        opts.saveForegroundPagesOnly = true
        opts.interpolationMode = InterpolationMode.NEAREST_NEIGHBOR
        opts.jpegQuality = 100
        opts.pixelOffsetMode = PixelOffsetMode.HIGH_SPEED
        opts.smoothingMode = SmoothingMode.HIGH_QUALITY

        page?.let {
            opts.pageIndex = (it - 1).coerceAtLeast(0)
        }

        return opts
    }

    companion object {

        private val REGEX_STRIP_TAGS = Regex("<.+?/>")
        private val REGEX_REPLACE_NR = Regex("[\\n\\r]+")

        init {
            val classLoader = this::class.java.classLoader
            val licenseAsStream = classLoader.getResourceAsStream(ASPOSE_LICENSE_FILE)
            com.aspose.diagram.License().setLicense(licenseAsStream)
        }
    }
}
