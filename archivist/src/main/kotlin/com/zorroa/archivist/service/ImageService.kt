package com.zorroa.archivist.service

import com.google.common.collect.ImmutableMap
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.WatermarkSettingsChanged
import com.zorroa.archivist.security.getUsername
import com.zorroa.archivist.security.hasPermission
import com.zorroa.archivist.util.FileUtils
import com.zorroa.archivist.util.event
import com.zorroa.common.schema.Proxy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.awt.*
import java.awt.image.BufferedImage
import java.io.*
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.annotation.PostConstruct
import javax.imageio.ImageIO
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.math.roundToInt

inline fun bufferedImageToInputStream(size: Int, img: BufferedImage) : InputStream {
    val ostream = object : ByteArrayOutputStream(size) {
        // Overriding this to not create a copy
        @Synchronized override fun toByteArray(): ByteArray {
            return this.buf
        }
    }
    ImageIO.write(img, "jpg", ostream)
    return ByteArrayInputStream(ostream.toByteArray())
}

/**
 * Created by chambers on 7/8/16.
 */
interface ImageService {

    @Throws(IOException::class)
    fun serveImage(req: HttpServletRequest, rsp: HttpServletResponse, storage: FileStorage, isWatermarkSize:Boolean)

    @Throws(IOException::class)
    fun serveImage(req: HttpServletRequest, rsp: HttpServletResponse, proxy: Proxy)

    fun watermark(req: HttpServletRequest, inputStream: InputStream): BufferedImage
}

/**
 * Created by chambers on 7/8/16.
 */
@Service
class ImageServiceImpl @Autowired constructor(
        private val fileStorageService: FileStorageService,
        private val fileServerProvider: FileServerProvider,
        private val properties: ApplicationProperties,
        private val eventBus: EventBus

) : ImageService {

    private var watermarkEnabled: Boolean = false
    private var watermarkMinProxySize: Int = 0
    private var watermarkTemplate: String = ""
    private var watermarkScale: Double = 1.0
    private var watermarkImage: BufferedImage? = null
    private var watermarkImageScale: Double = 0.2
    private var watermarkFontName : String = "Arial"

    @PostConstruct
    fun init() {
        setupWaterMarkResources(null)
        eventBus.register(this)
    }
    @Throws(IOException::class)
    override fun serveImage(req: HttpServletRequest, rsp: HttpServletResponse, storage: FileStorage, isWatermarkSize:Boolean) {
        if (storage == null) {
            rsp.status = HttpStatus.NOT_FOUND.value()
        }
        val file = fileServerProvider.getServableFile(storage)
        val stat = file.getStat()

        rsp.setHeader("Pragma", "")
        rsp.bufferSize = BUFFER_SIZE
        if (watermarkEnabled && isWatermarkSize) {
            val image = watermark(req, file.getInputStream())
            rsp.contentType = MediaType.IMAGE_JPEG_VALUE
            rsp.setHeader("Cache-Control", CacheControl.maxAge(1, TimeUnit.DAYS).cachePrivate().headerValue)
            ImageIO.write(image, "jpg", rsp.outputStream)

        } else {
            logger.event("serve Image", mapOf("mediaType" to stat.mediaType, "size" to stat.size))
            rsp.contentType = stat.mediaType
            rsp.setContentLengthLong(stat.size)
            rsp.setHeader("Cache-Control", CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate().headerValue)
            copyInputToOuput(file.getInputStream(), rsp.outputStream)
        }
    }

    @Throws(IOException::class)
    override fun serveImage(req: HttpServletRequest, rsp: HttpServletResponse, proxy: Proxy) {
        val isWatermarkSize = (proxy.width <= watermarkMinProxySize && proxy.height <= watermarkMinProxySize)
        val st = fileStorageService.get(proxy.id!!)
        serveImage(req, rsp, st, isWatermarkSize)
    }

    override fun watermark(req: HttpServletRequest, inputStream: InputStream): BufferedImage {
        val src = ImageIO.read(inputStream)
        val g2d = src.createGraphics()
        try {
            if (watermarkImage != null) {
                watermarkImage?.let {
                    val width = src.width.times(watermarkImageScale).toInt()
                    val image = it.getScaledInstance(width, -1, Image.SCALE_SMOOTH)
                    val xpos = src.width - image.getWidth(null) - 10
                    val ypos = src.height - image.getHeight(null) - 10
                    g2d.drawImage(image, xpos, ypos, null)
                }
            }
            else {
                val replacements = mapOf(
                        "USER" to getUsername(),
                        "DATE" to SimpleDateFormat("MM/dd/yyyy").format(Date()),
                        "IP" to (req.getHeader("X-FORWARDED-FOR") ?: req.remoteAddr),
                        "HOST" to req.remoteHost)

                val sb = StringBuffer(watermarkTemplate.length * 2)
                val m = PATTERN.matcher(watermarkTemplate)
                while (m.find()) {
                    try {
                        m.appendReplacement(sb, replacements[m.group(1)])
                    } catch (ignore: Exception) {
                        //
                    }
                }
                m.appendTail(sb)
                val text = sb.toString()

                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                val c = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f)
                g2d.composite = c
                g2d.font = getWatermarkFont(g2d, text, src.width)
                val x = ((src.width - g2d.getFontMetrics(g2d.font).stringWidth(text)) / 2).toFloat()
                val y = src.height - (1.1f * g2d.getFontMetrics(g2d.font).height)
                g2d.paint = Color.black
                g2d.drawString(text, x - 1, y + 1)
                g2d.drawString(text, x - 1, y - 1)
                g2d.drawString(text, x + 1, y + 1)
                g2d.drawString(text, x + 1, y -1)
                g2d.paint = Color.white
                g2d.drawString(text, x, y)
            }

        } finally {
            g2d.dispose()
        }
        return src
    }

    @Synchronized
    @Subscribe
    fun setupWaterMarkResources(e: WatermarkSettingsChanged?) {
        watermarkEnabled = properties.getBoolean("archivist.watermark.enabled")
        watermarkTemplate = properties.getString("archivist.watermark.template")
        watermarkMinProxySize = properties.getInt("archivist.watermark.min-proxy-size")
        watermarkScale = properties.getDouble("archivist.watermark.scale")
        watermarkFontName = properties.getString("archivist.watermark.font-name")
        watermarkImageScale = properties.getDouble("archivist.watermark.image-scale")

        val imagePath : String? = properties.getString("archivist.watermark.image-path")
        if (imagePath != null && imagePath.isNotBlank()) {
            try {
                logger.info("loading watermark image: '{}'", imagePath)
                watermarkImage = ImageIO.read(File(imagePath))
            } catch (e: Exception) {
                logger.warn("Failed to load watermark Image '{}'", imagePath, e)
            }
        }
        else {
            watermarkImage = null
        }
    }

    /**
     * Copy the contents of the given InputStream to the OutputStream.  Utilizes
     * NIO streams for max performance.
     *
     * @param input The src input stream
     * @param output The dst output stream
     */
    fun copyInputToOuput(input: InputStream, output: OutputStream) : Long {
        val inputChannel = Channels.newChannel(input)
        val outputChannel = Channels.newChannel(output)
        var size = 0L

        inputChannel.use { ic ->
            outputChannel.use { oc ->
                val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
                while (ic.read(buffer) != -1) {
                    buffer.flip()
                    size += oc.write(buffer)
                    buffer.clear()
                }
            }
        }
        return size
    }

    /**
     * Calculates the correct font size for the watermark based on the width and height of the
     * image and watermark scale. Returns the Font to use for the watermark.
     *
     * @param g2d the current graphics2D instance
     * @param text the string to calculate the size of
     * @param imageWidth the full image width
    */
    fun getWatermarkFont(g2d: Graphics2D, text: String, imageWidth: Int): Font {
        val baseFontSize = 20
        val baseFont = Font(watermarkFontName, Font.PLAIN, baseFontSize)
        val fontWidth = g2d.getFontMetrics(baseFont).stringWidth(text)
        val scale = (imageWidth.toFloat() * 0.75) / fontWidth.toFloat()
        var fontSize = baseFontSize.toFloat() * scale * watermarkScale
        return Font(watermarkFontName, Font.PLAIN, fontSize.toInt())
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ImageServiceImpl::class.java)

        /**
         * A table for converting the proxy type to a media type, which is required
         * to serve the proxy images properly.
         */
        val PROXY_MEDIA_TYPES: Map<String, MediaType> = ImmutableMap.of(
                "gif", MediaType.IMAGE_GIF,
                "jpg", MediaType.IMAGE_JPEG,
                "png", MediaType.IMAGE_PNG)

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
