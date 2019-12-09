package com.zorroa

import com.aspose.words.FontSettings
import com.aspose.words.ImageSaveOptions
import com.aspose.words.PdfSaveOptions
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.system.measureTimeMillis

class WordDocument(options: Options, inputStream: InputStream) : Document(options) {

    private val doc = com.aspose.words.Document(inputStream)

    init {
        if (options.verbose) {
            logFontsUsed()
            logAvailableFonts()
        }
    }

    private fun logFontsUsed() {
        logger.info("Fonts used in document:")
        for (fontInfo in doc.fontInfos) {
            logger.info("*** Font Name : " + fontInfo.name)
            logger.info("*** Font isTrueType  : " + fontInfo.isTrueType)
            logger.info("*** Font altName  : " + fontInfo.altName)
        }
    }

    private fun logAvailableFonts() {
        logger.info("Fonts available from default font source:")
        for (fontInfo in FontSettings.getDefaultInstance().fontsSources[0].availableFonts) {
            logger.info("*** FontFamilyName : " + fontInfo.fontFamilyName)
            logger.info("*** FullFontName  : " + fontInfo.fullFontName)
            logger.info("*** Version  : " + fontInfo.version)
            logger.info("*** FilePath : " + fontInfo.filePath)
        }
    }

    override fun renderAllImages(): Int {
        val pageCount = doc.pageCount
        for (page in 1..pageCount) {
            renderImage(page)
        }
        renderImage(0)
        return doc.pageCount + 1
    }

    override fun renderAllMetadata(): Int {

        val pageCount = doc.pageCount
        for (page in 1..pageCount) {
            renderMetadata(page)
        }
        renderMetadata(0)
        return doc.pageCount + 1
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
            if (page == 0) {
                val render = StackRender(
                    "MSWord", Color(52, 84, 148),
                    output.toInputStream()
                )

                ioHandler.writeImage(page, render.render())
            } else {
                ioHandler.writeImage(page, output)
            }
        }
        logImageTime(page, time)
    }

    override fun renderMetadata(page: Int) {
        val time = measureTimeMillis {
            val props = doc.builtInDocumentProperties
            val metadata = mutableMapOf<String, Any?>()

            if (page == 0) {
                metadata["type"] = "document"
                metadata["title"] = props.title
                metadata["author"] = props.author
                metadata["keywords"] = props.keywords
                metadata["timeCreated"] = convertDate(props.createdTime)
                metadata["length"] = doc.pageCount
            }

            val pageInfo = doc.getPageInfo((page - 1).coerceAtLeast(0))

            metadata["height"] = pageInfo.heightInPoints
            metadata["width"] = pageInfo.widthInPoints
            metadata["orientation"] = if (pageInfo.landscape) "landscape" else "portrait"

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
