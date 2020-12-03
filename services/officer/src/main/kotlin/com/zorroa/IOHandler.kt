package com.zorroa

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * A ByteArrayOutputStream which provides an efficient way to
 * obtain a BufferedInputStream without copying the internal byte buffer.
 */
class ReversibleByteArrayOutputStream(size: Int = 2048) : ByteArrayOutputStream(size) {

    fun toInputStream(): InputStream {
        return BufferedInputStream(ByteArrayInputStream(buf, 0, count))
    }
}

/**
 * Manages render inputs and outputs.
 */
class IOHandler(val options: RenderRequest) {

    fun writeImage(page: Int, outputStream: ReversibleByteArrayOutputStream) {
        StorageManager.storageClient().store(
            getImagePath(page),
            outputStream.toInputStream(),
            outputStream.size().toLong(),
            "image/jpeg"
        )
    }

    fun writeMetadata(page: Int, outputStream: ReversibleByteArrayOutputStream) {
        StorageManager.storageClient().store(
            getMetadataPath(page),
            outputStream.toInputStream(),
            outputStream.size().toLong(),
            "application/json"
        )
    }

    fun getImagePath(page: Int): String {
        return "${options.outputPath}_proxy.$page.jpg"
    }

    fun getMetadataPath(page: Int): String {
        return "${options.outputPath}_metadata.$page.json"
    }

    fun getOutputPath(): String {
        return options.outputPath
    }

    fun getMetadata(page: Int = 1): InputStream {
        return StorageManager.storageClient().fetch(getMetadataPath(page))
    }

    fun getImage(page: Int = 1): InputStream {
        return StorageManager.storageClient().fetch(getImagePath(page))
    }

    fun exists(page: Int = 1): Boolean {
        val path = getMetadataPath(page)
        return StorageManager.storageClient().exists(path)
    }

    fun removeImage(page: Int = 1) {
        StorageManager.storageClient().delete(getImagePath(page))
    }

    fun removeMetadata(page: Int = 1) {
        StorageManager.storageClient().delete(getMetadataPath(page))
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(IOHandler::class.java)

        // The size of the pre-allocated by array for images.
        val IMG_BUFFER_SIZE = 65536
    }
}
