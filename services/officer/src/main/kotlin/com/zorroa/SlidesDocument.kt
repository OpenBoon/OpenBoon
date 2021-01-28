package com.zorroa

import com.aspose.slides.Presentation
import com.aspose.slides.SlideUtil
import java.io.InputStream
import javax.imageio.ImageIO
import kotlin.system.measureTimeMillis

class SlidesDocument(options: RenderRequest, inputStream: InputStream) : Document(options) {

    private val doc = Presentation(inputStream)

    override fun renderAllImages(): Int {
        for (page in 0 until doc.slides.size()) {
            renderImage(page + 1)
        }
        return doc.slides.size()
    }

    override fun pageCount(): Int {
        return doc.slides.size()
    }

    override fun renderAllMetadata(): Int {
        for (page in 0 until doc.slides.size()) {
            renderMetadata(page + 1)
        }
        return doc.slides.size()
    }

    override fun renderImage(page: Int) {
        // slides are zero based
        val time = measureTimeMillis {
            val sld = doc.slides.get_Item((page - 1).coerceAtLeast(0))
            val image = sld.getThumbnail(1f, 1f)
            val output = ReversibleByteArrayOutputStream(IOHandler.IMG_BUFFER_SIZE)
            ImageIO.write(image, "jpeg", output)
            ioHandler.writeImage(page, output)
        }
        logImageTime(page, time)
    }

    override fun renderMetadata(page: Int) {
        val time = measureTimeMillis {
            val props = doc.documentProperties
            val metadata = mutableMapOf<String, Any?>()
            val dim = doc.presentation.slideSize

            metadata["type"] = "document"
            metadata["title"] = props.title
            metadata["author"] = props.author
            metadata["keywords"] = props.keywords
            metadata["description"] = props.category
            metadata["timeCreated"] = convertDate(props.createdTime)
            metadata["length"] = doc.slides.size()
            metadata["pageNumber"] = page
            metadata["height"] = dim.size.width
            metadata["width"] = dim.size.height
            metadata["orientation"] = if (dim.size.height > dim.size.width) "portrait" else "landscape"
            metadata["content"] = extractPageContent(page)

            val output = ReversibleByteArrayOutputStream()
            Json.mapper.writeValue(output, metadata)
            ioHandler.writeMetadata(page, output)
        }
        logMetadataTime(page, time)
    }

    private fun extractPageContent(page: Int): String? {

        val slide = doc.slides.get_Item(page - 1)
        val auxPresentation = Presentation()
        auxPresentation.slides.insertClone(0, slide)

        val sb = StringBuilder(1024)
        for (frame in SlideUtil.getAllTextFrames(auxPresentation, false)) {
            sb.append(frame.text)
            sb.append(" ")
        }
        return sb.toString().replace(Document.whitespaceRegex, " ")
    }

    override fun close() {
        try {
            doc.dispose()
        } catch (e: Exception) {
            // ignore
        }
    }

    companion object {
        init {
            val classLoader = this::class.java.classLoader
            val licenseAsStream = classLoader.getResourceAsStream(ASPOSE_LICENSE_FILE)
            com.aspose.slides.License().setLicense(licenseAsStream)
        }
    }
}
