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
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.annotation.PostConstruct
import javax.imageio.ImageIO
import javax.servlet.http.HttpServletRequest
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
    fun serveImage(req: HttpServletRequest, storage: FileStorage): ResponseEntity<InputStreamResource>

    @Throws(IOException::class)
    fun serveImage(req: HttpServletRequest, proxy: Proxy): ResponseEntity<InputStreamResource>

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
    override fun serveImage(req: HttpServletRequest, storage: FileStorage): ResponseEntity<InputStreamResource> {
        if (storage == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }
        val file = fileServerProvider.getServableFile(storage)

        return if (watermarkEnabled && !hasPermission("zorroa::export")) {
            val image = watermark(req, file.getInputStream())
            // make a ByteArrayOutputStream which doesn't create a defensive copy.
            val ostream = object : ByteArrayOutputStream(storage.size.toInt()) {
                @Synchronized override fun toByteArray(): ByteArray {
                    return this.buf
                }
            }
            // Write the image to the stream
            ImageIO.write(image, "jpg", ostream)
            val bytes = ostream.toByteArray()

            ResponseEntity.ok()
                    .contentLength(bytes.size.toLong())
                    .contentType(MediaType.IMAGE_JPEG)
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
                    .body(InputStreamResource(ByteArrayInputStream(bytes)))
        } else {
            ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(storage.mimeType))
                    .contentLength(storage.size)
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
                    .body(InputStreamResource(file.getInputStream()))
        }
    }

    @Throws(IOException::class)
    override fun serveImage(req: HttpServletRequest, proxy: Proxy): ResponseEntity<InputStreamResource> {
        val st = fileStorageService.get(proxy.id!!)
        return serveImage(req, st)
    }

    override fun watermark(req: HttpServletRequest, inputStream: InputStream): BufferedImage {
        val src = ImageIO.read(inputStream)
        if (src.width <= watermarkMinProxySize && src.height <= watermarkMinProxySize) {
            return src
        }

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
                        "IP" to req.remoteAddr,
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

                val fontSize = getWatermarkFontSize(src.width, src.height, text.length)
                val watermarkFont = Font(watermarkFontName, Font.PLAIN, fontSize)

                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                val c = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f)
                g2d.composite = c

                g2d.font = watermarkFont
                val x = ((src.width - g2d.getFontMetrics(watermarkFont).stringWidth(text)) / 2).toFloat()
                val y = src.height - (1.1f * g2d.getFontMetrics(watermarkFont).height)
                g2d.paint = Color.black
                g2d.drawString(text, x - 1, y + 1)
                g2d.drawString(text, x - 1, y - 1)
                g2d.drawString(text, x + 1, y + 1)
                g2d.drawString(text, x + 1, y - 1)
                g2d.paint = Color.white
                g2d.drawString(text, x, y)
            }

        } finally {
            g2d.dispose()
        }
        return src
    }

    fun getWatermarkFontSize(width: Int, height: Int, characterLength: Int): Int {
        /*
        Calculates the correct font size for the watermark based on the width and height of the image and the number of
        characters in the text.
         */
        var fontSize = (width * 1.5 / characterLength * watermarkScale).roundToInt()
        if (fontSize > 96) {
            fontSize = 96
        }
        if (fontSize > height) {
            fontSize = height
        }
        return fontSize
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

    companion object {

        private val logger = LoggerFactory.getLogger(ImageServiceImpl::class.java)

        /**
         * The pattern used to define text replacements in the watermark texts
         */
        private val PATTERN = Pattern.compile("#\\[(.*?)\\]")
    }
}
