package com.zorroa

import com.aspose.ocr.ImageStream
import com.aspose.ocr.ImageStreamFormat
import com.aspose.ocr.OcrEngine
import java.io.InputStream
import kotlin.system.measureTimeMillis

/**
 * OCR is ridiculously slow and shouldn't be used.
 */
class OcrDocument(options: Options, inputStream: InputStream) : Document(options) {

    private val ocr = OcrEngine()

    init {
        ocr.processAllPages = false
        ocr.setImage(ImageStream.fromStream(inputStream, detectFormat()))
    }

    private fun detectFormat() : Int {
        val name = options.fileName.toLowerCase()
        return when {
            name.endsWith(".tif") -> {
                ImageStreamFormat.Tiff
            }
            name.endsWith(".tiff") -> {
                ImageStreamFormat.Tiff
            }
            name.endsWith(".gif") -> {
                ImageStreamFormat.Gif
            }
            name.endsWith(".jpg") -> {
                ImageStreamFormat.Jpg
            }
            name.endsWith(".png") -> {
                ImageStreamFormat.Png
            }
            name.endsWith(".bmp") -> {
                ImageStreamFormat.Bmp
            }
            else -> {
                throw IllegalArgumentException("Invalid file type: $name")
            }
        }
    }

    override fun renderImage(page: Int) {}

    override fun renderAllImages() {}

    override fun renderAllMetadata() {
        val time = measureTimeMillis {
            if (ocr.process()) {
                val output = ReversibleByteArrayOutputStream()
                for ((index, page) in ocr.pages.withIndex()) {
                    val content = mapOf("content" to page.pageText.toString())
                    Json.mapper.writeValue(output, content)
                    ioHandler.writeMetadata(index+1, output)
                    output.reset()
                    logMetadataTime(index + 1, 0)
                }
            }
        }
        logger.info("input file='${options.fileName}' all pages in time='{}ms'", time)
    }

    override fun renderMetadata(page: Int) {
        val time = measureTimeMillis {
            if (ocr.process()) {
                val ocrPage = ocr.pages[page - 1]
                val content = mapOf("content" to ocrPage.pageText.toString())
                val output = ReversibleByteArrayOutputStream()
                Json.mapper.writeValue(output, content)
                ioHandler.writeMetadata(page, output)
            }
        }
        logMetadataTime(page, time)
    }

    override fun close() {
        ocr.dispose()
    }

    companion object {
        init {
            val classLoader = this::class.java.classLoader
            val licenseAsStream = classLoader.getResourceAsStream(ASPOSE_LICENSE_FILE)
            com.aspose.ocr.License().setLicense(licenseAsStream)
        }
    }
}