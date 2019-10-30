package com.zorroa

import com.aspose.ocr.ImageStream
import com.aspose.ocr.OcrEngine
import kotlin.system.measureTimeMillis

class OcrDocument(options: Options) : Document(options) {

    private val ocr = OcrEngine()

    init {
        ocr.processAllPages = true
        ocr.setImage(ImageStream.fromFile(ioHandler.getInputPath()))
    }

    override fun renderImage(page: Int) {}

    override fun renderAllImages() {}

    override fun renderAllMetadata() {
        val time = measureTimeMillis {
            if (ocr.process()) {
                for ((index, page) in ocr.pages.withIndex()) {
                    val content = mapOf("content" to page.pageText.toString())
                    Json.mapper.writeValue(getMetadataFile(index + 1), content)
                    logMetadataTime(index + 1, 0)
                }
            }
        }
        logger.info("input file='${options.inputFile}' all pages in time='{}ms'", time)
    }

    override fun renderMetadata(page: Int) {
        val time = measureTimeMillis {
            if (ocr.process()) {
                val ocrPage = ocr.pages[page - 1]
                val content = mapOf("content" to ocrPage.pageText.toString())
                Json.mapper.writeValue(getMetadataFile(page), content)
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