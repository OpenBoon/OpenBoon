package com.zorroa

import com.aspose.words.ImageSaveOptions
import com.aspose.words.PdfSaveOptions
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.system.measureTimeMillis

class WordDocument(options: RenderRequest, inputStream: InputStream) : Document(options) {

    private val doc = com.aspose.words.Document(inputStream)

    private fun logFontsUsed() {
        logger.info("Fonts used in document:")
        for (fontInfo in doc.fontInfos) {
            logger.info("*** Font Name : " + fontInfo.name)
            logger.info("*** Font isTrueType  : " + fontInfo.isTrueType)
            logger.info("*** Font altName  : " + fontInfo.altName)
        }
    }

    override fun renderAllImages(): Int {
        val pageCount = doc.pageCount
        for (page in 1..pageCount) {
            renderImage(page)
        }
        return doc.pageCount
    }

    override fun pageCount(): Int {
        return doc.pageCount
    }

    override fun renderAllMetadata(): Int {

        val pageCount = doc.pageCount
        for (page in 1..pageCount) {
            renderMetadata(page)
        }
        return doc.pageCount
    }

    override fun renderImage(page: Int) {
        val time = measureTimeMillis {
            val imageSaveOptions = ImageSaveOptions(com.aspose.words.SaveFormat.JPEG)
            imageSaveOptions.horizontalResolution = 96f
            imageSaveOptions.verticalResolution = 96f
            imageSaveOptions.pageCount = 1
            imageSaveOptions.pageIndex = (page - 1).coerceAtLeast(0)

            val output = ReversibleByteArrayOutputStream(IOHandler.IMG_BUFFER_SIZE)
            doc.save(output, imageSaveOptions)
            ioHandler.writeImage(page, output)
        }
        logImageTime(page, time)
    }

    override fun renderMetadata(page: Int) {
        val time = measureTimeMillis {
            val props = doc.builtInDocumentProperties
            val metadata = mutableMapOf<String, Any?>()

            metadata["type"] = "document"
            metadata["title"] = props.title
            metadata["author"] = props.author
            metadata["keywords"] = props.keywords
            metadata["timeCreated"] = convertDate(props.createdTime)
            metadata["length"] = doc.pageCount
            metadata["pageNumber"] = page

            val pageInfo = doc.getPageInfo((page - 1).coerceAtLeast(0))
            metadata["height"] = pageInfo.heightInPoints
            metadata["width"] = pageInfo.widthInPoints
            metadata["orientation"] = if (pageInfo.landscape) "landscape" else "portrait"
            metadata["content"] = extractPageContent(page)

            val output = ReversibleByteArrayOutputStream()
            Json.mapper.writeValue(output, metadata)
            ioHandler.writeMetadata(page, output)
        }
        logMetadataTime(page, time)
    }

    private fun extractPageContent(page: Int): String? {

        val byteStream = ByteArrayOutputStream()
        val saveOptions = PdfSaveOptions()
        saveOptions.pageIndex = page - 1
        saveOptions.pageCount = 1
        doc.save(byteStream, saveOptions)

        // save the page as pdf then extract content from 1 page pdf
        val text = PdfDocument.extractPdfText(byteStream.toByteArray())

        return text.replace(Document.whitespaceRegex, " ")
    }

    override fun close() {
        try {
            doc.cleanup()
        } catch (e: Exception) {
            // ignore
        }
    }

    companion object {
        init {
            val classLoader = this::class.java.classLoader
            val licenseAsStream = classLoader.getResourceAsStream(ASPOSE_LICENSE_FILE)
            com.aspose.words.License().setLicense(licenseAsStream)
        }
    }
}
