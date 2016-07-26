package com.zorroa.analyst.web;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.filesystem.ObjectFileSystem;
import com.zorroa.sdk.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.HandlerMapping;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Provides endpoints for downloading proxy images.
 *
 * @author chambers
 *
 */
@Controller
@Component
public class FileSystemController {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemController.class);

    private static final int CACHE_MAX_SIZE = 100;
    private static final int CACHE_TIMEOUT_MINUTES = 1;

    private final LoadingCache<String, ProxyImage> proxyCache = CacheBuilder.newBuilder()
        .maximumSize(CACHE_MAX_SIZE)
        .concurrencyLevel(4)
        .expireAfterWrite(CACHE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        .build(new CacheLoader<String, ProxyImage>() {
         public ProxyImage load(String key) throws Exception {
             return loadProxyImage(key);
         }
    });

    @Autowired
    AssetDao assetDao;

    @Autowired
    ObjectFileSystem objectFileSystem;

    @Value("${analyst.watermark.enabled:false}")
    private Boolean watermarkEnabled;

    @Value("${analyst.watermark.min-proxy-width:384}")
    private int watermarkMinProxyWidth;

    @Value("${analyst.watermark.template}")
    private String watermarkTemplate;

    @Value("${analyst.watermark.font-size:6}")
    private int watermarkFontSize;

    @Value("${analyst.watermark.font-name:Arial Black}")
    private String watermarkFontName;
    private Font watermarkFont;

    @PostConstruct
    public void init() {
        watermarkFont = new Font(watermarkFontName, Font.PLAIN, watermarkFontSize);
    }

    /**
     * The proxies URL is broken out rather than using the general purpose getFile method because
     * proxies are handled differently due to caching.
     *
     * @param request
     * @param response
     * @return
     * @throws ExecutionException
     */
    @RequestMapping(value = "/api/v1/fs/proxies/**", method = RequestMethod.GET, produces = { MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_GIF_VALUE })
    @ResponseBody
    public byte[] getProxy(HttpServletRequest request, HttpServletResponse response) throws ExecutionException, IOException {
        response.setHeader("Cache-Control", "public");

        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        AntPathMatcher apm = new AntPathMatcher();
        String finalPath = "proxies/" + FileUtils.filename(apm.extractPathWithinPattern(bestMatchPattern, path));

        ProxyImage image = proxyCache.get(finalPath);
        String ext = FileUtils.extension(path);
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        ImageIO.write(watermark(image.image), ext, bao);
        return bao.toByteArray();
    }

    /**
     * Stream the given asset ID.
     *
     * @param response
     * @return
     * @throws ExecutionException
     * @throws FileNotFoundException
     */
    @RequestMapping(value = "/api/v1/assets/{id}/_stream", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<FileSystemResource> streamAsset(@PathVariable String id, HttpServletResponse response) throws ExecutionException, IOException {
        Asset asset = assetDao.get(id);
        if (!new File(asset.getSource().getPath()).exists()) {
            response.sendRedirect(asset.getAttr("source:remoteSourceUri"));
            return null;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(asset.getSource().getMediaType()))
                .contentLength(asset.getSource().getFileSize())
                .body(new FileSystemResource(asset.getSource().getPath()));
    }

    private ProxyImage loadProxyImage(String path) throws IOException {
        return ProxyImage.load(objectFileSystem.get(path).getFile());
    }

    private static class ProxyImage {
        public BufferedImage image;

        public static final ProxyImage load(File file) throws IOException {
            ProxyImage result = new ProxyImage();
            result.image = ImageIO.read(file);
            return result;
        }
    }

    private BufferedImage watermark(BufferedImage src) {
        if (!watermarkEnabled) {
            return src;
        }

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
