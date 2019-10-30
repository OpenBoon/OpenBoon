package com.zorroa

import com.aspose.slides.Presentation
import com.aspose.slides.SlideUtil
import javax.imageio.ImageIO
import kotlin.system.measureTimeMillis

class SlidesDocument(options: Options) : Document(options) {

    private val doc = Presentation(ioHandler.getInputPath())

    override fun renderAllImages() {
        for (page in 0 until doc.slides.size()) {
            renderImage(page + 1)
        }
    }

    override fun renderAllMetadata() {
        for (page in 0 until doc.slides.size()) {
            renderMetadata(page + 1)
        }
    }

    override fun renderImage(page: Int) {
        // slides are zero based
        val time = measureTimeMillis {
            val sld = doc.slides.get_Item(page - 1)
            val image = sld.getThumbnail(1f, 1f)
            ImageIO.write(image, "jpeg", ioHandler.getImagePath(page).toFile())
        }
        logImageTime(page, time)
    }

    override fun renderMetadata(page: Int) {
        val time = measureTimeMillis {
            val props = doc.documentProperties
            val metadata = mutableMapOf<String, Any?>()
            val dim = doc.presentation.slideSize

            metadata["title"] = props.title
            metadata["author"] = props.author
            metadata["keywords"] = props.keywords
            metadata["description"] = props.category
            metadata["timeCreated"] = try {
                props.createdTime
            } catch (e: Exception) {
                null
            }
            metadata["timeModified"] = try {
                props.lastSavedTime
            } catch (e: Exception) {
                null
            }
            metadata["pages"] = doc.slides.size()
            metadata["height"] = dim.size.width
            metadata["width"] = dim.size.height
            metadata["orientation"] = if (dim.size.height > dim.size.width) "portrait" else "landscape"

            if (options.content) {
                metadata["content"] = extractPageContent(page)
            }

            Json.mapper.writeValue(getMetadataFile(page), metadata)
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
