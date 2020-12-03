package com.zorroa

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.InputStream
import java.util.Date

/**
 * The minimal Document interface.
 */
abstract class Document(val options: RenderRequest) : Closeable {

    val ioHandler = IOHandler(options)

    fun renderImage() {
        renderImage(options.page)
    }

    abstract fun renderImage(page: Int)
    abstract fun renderAllImages(): Int

    fun renderMetadata() {
        renderMetadata(options.page)
    }

    abstract fun renderAllMetadata(): Int
    abstract fun renderMetadata(page: Int)

    fun render() {
        if (isRenderAll()) {
            logger.info("Rendering to {}", options.outputPath)
            if (!options.disableImageRender) {
                renderAllImages()
            }
            renderAllMetadata()
        } else {
            if (!options.disableImageRender) {
                renderImage()
            }
            renderMetadata()
        }
    }

    fun isRenderAll(): Boolean {
        return options.page < 0
    }

    fun logImageTime(page: Int, time: Long) {
        val mem = Runtime.getRuntime().freeMemory() / 1024 / 1024
        logger.info(
            "proxy input='${options.fileName}' output='${ioHandler.getOutputPath()}' page='$page' in time='{}ms', freemem='{}m'",
            time,
            mem
        )
    }

    fun logMetadataTime(page: Int, time: Long) {
        val mem = Runtime.getRuntime().freeMemory() / 1024 / 1024
        logger.info(
            "metadata input='${options.fileName}'  output='${ioHandler.getOutputPath()}' page='$page' in time='{}ms', freemem='{}m'",
            time,
            mem
        )
    }

    fun getMetadata(page: Int): InputStream {
        if (page < 1) {
            throw IllegalArgumentException("Page number cannot be less than 1")
        }
        return ioHandler.getMetadata(page)
    }

    fun getImage(page: Int): InputStream {
        if (page < 1) {
            throw IllegalArgumentException("Page number cannot be less than 1")
        }
        return ioHandler.getImage(page)
    }

    fun convertDate(date: Date?): String? {
        if (date == null) {
            return null
        }
        return try {
            date.toInstant()?.toString()
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(Document::class.java)
        val whitespaceRegex = Regex("\\s+")
    }
}
