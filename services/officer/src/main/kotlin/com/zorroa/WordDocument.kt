package com.zorroa

import com.aspose.words.FontSettings
import com.aspose.words.ImageSaveOptions
import com.aspose.words.PdfSaveOptions
import java.io.ByteArrayOutputStream
import kotlin.system.measureTimeMillis

class WordDocument(options: Options) : Document(options) {

    private val doc = com.aspose.words.Document(ioHandler.getInputPath())

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

    override fun renderAllImages() {
        val pageCount = doc.pageCount
        for (page in 1..pageCount) {
            renderImage(page)
        }
    }

    override fun renderAllMetadata() {
        val pageCount = doc.pageCount
        for (page in 1..pageCount) {
            renderMetadata(page)
        }
    }

    override fun renderImage(page: Int) {
        val time = measureTimeMillis {
            val imageSaveOptions = ImageSaveOptions(com.aspose.words.SaveFormat.JPEG)
            imageSaveOptions.horizontalResolution = 96f
            imageSaveOptions.verticalResolution = 96f
            imageSaveOptions.pageCount = 1
            imageSaveOptions.pageIndex = page - 1
            doc.save(ioHandler.getImagePath(page).toString(), imageSaveOptions)
        }
        logImageTime(page, time)
    }

    override fun renderMetadata(page: Int) {
        val time = measureTimeMillis {
            val props = doc.builtInDocumentProperties
            val metadata = mutableMapOf<String, Any?>()

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
            metadata["pages"] = doc.pageCount

            val pageInfo = doc.getPageInfo(page - 1)
            metadata["height"] = pageInfo.heightInPoints
            metadata["width"] = pageInfo.widthInPoints
            metadata["orientation"] = if (pageInfo.landscape) "landscape" else "portrait"

            if (options.content) {
                metadata["content"] = extractPageContent(page)
            }

            Json.mapper.writeValue(getMetadataFile(page), metadata)
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
