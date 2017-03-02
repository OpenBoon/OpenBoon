package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.zorroa.sdk.domain.Proxy;
import com.zorroa.sdk.filesystem.ObjectFileSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.Map;

/**
 * Created by chambers on 7/8/16.
 */
@Service
public class ImageServiceImpl implements ImageService {

    @Value("${archivist.watermark.enabled:false}")
    private Boolean watermarkEnabled;

    @Value("${archivist.watermark.min-proxy-width:384}")
    private int watermarkMinProxyWidth;

    @Value("${archivist.watermark.template}")
    private String watermarkTemplate;

    @Value("${archivist.watermark.font-size:6}")
    private int watermarkFontSize;

    @Value("${archivist.watermark.font-name:Arial Black}")
    private String watermarkFontName;
    private Font watermarkFont;

    @Autowired
    ObjectFileSystem objectFileSystem;

    /**
     * A table for converting the proxy type to a media type, which is required
     * to serve the proxy images properly.
     */
    public static final Map<String, MediaType> PROXY_MEDIA_TYPES = ImmutableMap.of(
            "gif", MediaType.IMAGE_GIF,
            "jpg", MediaType.IMAGE_JPEG,
            "png", MediaType.IMAGE_PNG);

    @PostConstruct
    public void init() {
        watermarkFont = new Font(watermarkFontName, Font.PLAIN, watermarkFontSize);
    }

    @Override
    public ResponseEntity<InputStreamResource> serveImage(File file) throws IOException {
        String ext = com.zorroa.sdk.util.FileUtils.extension(file);
        if (watermarkEnabled) {
            ByteArrayOutputStream output = watermark(file, ext);
            return ResponseEntity.ok()
                    .contentType(PROXY_MEDIA_TYPES.get(ext))
                    .contentLength(output.size())
                    .header("Cache-Control", "public")
                    .body(new InputStreamResource(new ByteArrayInputStream(output.toByteArray(), 0, output.size())));
        }
        else {
            return ResponseEntity.ok()
                    .contentType(PROXY_MEDIA_TYPES.get(ext))
                    .contentLength(Files.size(file.toPath()))
                    .header("Cache-Control", "public")
                    .body(new InputStreamResource(new FileInputStream(file)));
        }
    }

    @Override
    public ResponseEntity<InputStreamResource> serveImage(Proxy proxy) throws IOException {
        return serveImage(objectFileSystem.get(proxy.getId()).getFile());
    }

    @Override
    public ByteArrayOutputStream watermark(File file, String format) throws IOException {
        BufferedImage image = watermark(ImageIO.read(file));
        final ByteArrayOutputStream output = new ByteArrayOutputStream() {
            @Override
            public synchronized byte[] toByteArray() {
                return this.buf;
            }
        };
        ImageIO.write(image, format, output);
        return output;
    }

    @Override
    public BufferedImage watermark(BufferedImage src) {

        if (src.getWidth() <= watermarkMinProxyWidth) {
            return src;
        }

        // Replace variables in the watermarkTemplte once we have session & asset
        String text = watermarkTemplate;

        // Draw AA text composited on top of the image
        // FIXME: Wrap strings that are too long
        Graphics2D g2d = src.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Composite c = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
        g2d.setComposite(c);
        g2d.setPaint(Color.white);
        g2d.setFont(watermarkFont);
        float x = (src.getWidth() - g2d.getFontMetrics(watermarkFont).stringWidth(text)) / 2;
        float y = 1.1f * g2d.getFontMetrics(watermarkFont).getHeight();
        g2d.drawString(text, x, y);

        return src;
    }
}
