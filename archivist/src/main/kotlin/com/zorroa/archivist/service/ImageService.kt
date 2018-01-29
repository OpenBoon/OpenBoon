package com.zorroa.archivist.service

import com.google.common.collect.ImmutableMap
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.zorroa.archivist.domain.WatermarkSettingsChanged
import com.zorroa.archivist.security.SecurityUtils
import com.zorroa.common.config.ApplicationProperties
import com.zorroa.sdk.domain.Proxy
import com.zorroa.sdk.filesystem.ObjectFileSystem
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.*
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.annotation.PostConstruct
import javax.imageio.ImageIO

/**
 * Created by chambers on 7/8/16.
 */
interface ImageService {
    @Throws(IOException::class)
    fun serveImage(file: File): ResponseEntity<InputStreamResource>

    @Throws(IOException::class)
    fun serveImage(proxy: Proxy): ResponseEntity<InputStreamResource>

    @Throws(IOException::class)
    fun watermark(file: File, format: String): ByteArrayOutputStream

    fun watermark(src: BufferedImage): BufferedImage
}

/**
 * Created by chambers on 7/8/16.
 */
@Service
class ImageServiceImpl @Autowired constructor(
        private val objectFileSystem: ObjectFileSystem,
        private val properties: ApplicationProperties,
        private val eventBus: EventBus

) : ImageService {

    private var watermarkEnabled: Boolean = false
    private var watermarkMinProxySize: Int = 0
    private var watermarkTemplate: String = ""
    private var watermarkFont: Font? = null

    @PostConstruct
    fun init() {
        setupWaterMarkFont(null)
        eventBus.register(this)
    }

    @Throws(IOException::class)
    override fun serveImage(file: File): ResponseEntity<InputStreamResource> {
        val ext = com.zorroa.sdk.util.FileUtils.extension(file)
        return if (watermarkEnabled) {
            val output = watermark(file, ext)
            ResponseEntity.ok()
                    .contentType(PROXY_MEDIA_TYPES[ext])
                    .contentLength(output.size().toLong())
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
                    .body(InputStreamResource(ByteArrayInputStream(output.toByteArray(), 0, output.size())))
        } else {
            ResponseEntity.ok()
                    .contentType(PROXY_MEDIA_TYPES[ext])
                    .contentLength(Files.size(file.toPath()))
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
                    .body(InputStreamResource(FileInputStream(file)))
        }
    }

    @Throws(IOException::class)
    override fun serveImage(proxy: Proxy): ResponseEntity<InputStreamResource> {
        return serveImage(objectFileSystem.get(proxy.id).file)
    }

    @Throws(IOException::class)
    override fun watermark(file: File, format: String): ByteArrayOutputStream {
        val image = watermark(ImageIO.read(file))
        val output = object : ByteArrayOutputStream() {
            @Synchronized override fun toByteArray(): ByteArray {
                return this.buf
            }
        }
        ImageIO.write(image, format, output)
        return output
    }

    override fun watermark(src: BufferedImage): BufferedImage {
        if (src.width <= watermarkMinProxySize && src.height <= watermarkMinProxySize) {
            return src
        }

        val replacements = ImmutableMap.of(
                "USER", SecurityUtils.getUsername(),
                "DATE", SimpleDateFormat("MM/dd/yyyy").format(Date()))

        val sb = StringBuffer(watermarkTemplate.length * 2)
        val m = PATTERN.matcher(watermarkTemplate)
        while (m.find()) {
            m.appendReplacement(sb, replacements[m.group(1)])
        }
        m.appendTail(sb)
        val text = sb.toString()

        // FIXME: Wrap strings that are too long
        val g2d = src.createGraphics()
        try {
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            val c = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f)
            g2d.composite = c
            g2d.paint = Color.white
            g2d.font = watermarkFont
            val x = ((src.width - g2d.getFontMetrics(watermarkFont).stringWidth(text)) / 2).toFloat()
            val y = 1.1f * g2d.getFontMetrics(watermarkFont).height
            g2d.drawString(text, x, src.height - y)
        } finally {
            g2d.dispose()
        }
        return src
    }

    @Synchronized
    @Subscribe
    fun setupWaterMarkFont(e: WatermarkSettingsChanged?) {
        watermarkEnabled = properties.getBoolean("archivist.watermark.enabled")
        watermarkTemplate = properties.getString("archivist.watermark.template")
        watermarkMinProxySize = properties.getInt("archivist.watermark.min-proxy-size")

        val fontName = properties.getString("archivist.watermark.font-name")
        val fontSize = properties.getInt("archivist.watermark.font-size")
        watermarkFont = Font(fontName, Font.PLAIN, fontSize)
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
         * The pattern used to define text replacements in the watermark texts
         */
        private val PATTERN = Pattern.compile("#\\[(.*?)\\]")
    }
}
