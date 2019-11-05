package com.zorroa.archivist.service

import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.schema.Proxy
import com.zorroa.archivist.util.copyInputToOuput
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.awt.Dimension
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.imageio.ImageIO
import javax.imageio.stream.FileImageInputStream
import javax.servlet.http.HttpServletResponse

/**
 * Created by chambers on 7/8/16.
 */
interface ImageService {

    /**
     * Stream the given stored file to the [HttpServletResponse].
     *
     * @param rsp The [HttpServletResponse] to stream to.
     * @param storage The stored file.
     * @param isWatermarkSize Set to True if the image should be watermarked.
     */
    @Throws(IOException::class)
    fun serveImage(rsp: HttpServletResponse, storage: FileStorage, isWatermarkSize: Boolean)

    /**
     * Stream the given Proxy file to the provided [HttpServletResponse]
     *
     * @param rsp The [HttpServletResponse] to stream to.
     * @param proxy The [Proxy] to stram.
     */
    @Throws(IOException::class)
    fun serveImage(rsp: HttpServletResponse, proxy: Proxy?)

    /**
     * Stream the given stored file to the [HttpServletResponse]
     *
     * @param rsp The [HttpServletResponse] to stream to.
     * @param storage The stored file.
     */
    @Throws(IOException::class)
    fun serveImage(rsp: HttpServletResponse, storage: FileStorage)

    /**
     * Find the dimensions for the given image without loading the
     * whole image into memory.
     *
     * @param imgFile A file pointing to an image.
     * @return a [Dimension] representing the size of the image.
     */
    fun getImageDimension(imgFile: File): Dimension
}

/**
 * Created by chambers on 7/8/16.
 */
@Service
class ImageServiceImpl @Autowired constructor(
    private val fileStorageService: FileStorageService,
    private val fileServerProvider: FileServerProvider

) : ImageService {

    @Throws(IOException::class)
    override fun serveImage(
        rsp: HttpServletResponse,
        storage: FileStorage
    ) {

        val file = fileServerProvider.getServableFile(storage)
        serveImage(rsp, storage, false)
    }

    @Throws(IOException::class)
    override fun serveImage(
        rsp: HttpServletResponse,
        storage: FileStorage,
        isWatermarkSize: Boolean
    ) {
        if (storage == null) {
            rsp.status = HttpStatus.NOT_FOUND.value()
        }
        val file = fileServerProvider.getServableFile(storage)
        val stat = file.getStat()

        rsp.setHeader("Pragma", "")
        rsp.bufferSize = 8192
        rsp.contentType = stat.mediaType
        rsp.setContentLengthLong(stat.size)
        rsp.setHeader("Cache-Control", CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate().headerValue)
        copyInputToOuput(file.getInputStream(), rsp.outputStream)
    }

    @Throws(IOException::class)
    override fun serveImage(rsp: HttpServletResponse, proxy: Proxy?) {
        if (proxy == null) {
            rsp.status = HttpStatus.NOT_FOUND.value()
            return
        }
        val st = fileStorageService.get(proxy.id)
        serveImage(rsp, st)
    }

    override fun getImageDimension(imgFile: File): Dimension {
        val pos = imgFile.name.lastIndexOf(".")
        if (pos == -1)
            throw RuntimeException("No extension for file: " + imgFile.absolutePath)
        val suffix = imgFile.name.substring(pos + 1)
        val iter = ImageIO.getImageReadersBySuffix(suffix)
        while (iter.hasNext()) {
            val reader = iter.next()
            try {
                val stream = FileImageInputStream(imgFile)
                reader.input = stream
                val width = reader.getWidth(reader.minIndex)
                val height = reader.getHeight(reader.minIndex)
                return Dimension(width, height)
            } catch (e: IOException) {
                logger.warn("Error reading: ${imgFile.absolutePath}", e)
            } finally {
                reader.dispose()
            }
        }

        throw IOException("Not a known image file: " + imgFile.absolutePath)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ImageServiceImpl::class.java)

        /**
         * Output stream buffer size
         */
        const val BUFFER_SIZE = 16 * 1024

        /**
         * The pattern used to define text replacements in the watermark texts
         */
        private val PATTERN = Pattern.compile("#\\[(.*?)\\]")
    }
}
