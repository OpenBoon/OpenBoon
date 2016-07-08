package com.zorroa.archivist.web;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.zorroa.archivist.service.ImageService;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.sdk.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.HandlerMapping;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
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

    @Autowired
    AssetDao assetDao;

    @Autowired
    ImageService imageService;

    private final LoadingCache<String, ProxyImage> proxyCache = CacheBuilder.newBuilder()
        .maximumSize(CACHE_MAX_SIZE)
        .concurrencyLevel(4)
        .expireAfterWrite(CACHE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        .build(new CacheLoader<String, ProxyImage>() {
         public ProxyImage load(String key) throws Exception {
             return loadProxyImage(key);
         }
    });

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
        ImageIO.write(imageService.watermark(image.image), ext, bao);
        return bao.toByteArray();
    }

    private ProxyImage loadProxyImage(String path) throws IOException {
        return null;
    }

    private static class ProxyImage {
        public BufferedImage image;

        public static final ProxyImage load(File file) throws IOException {
            ProxyImage result = new ProxyImage();
            result.image = ImageIO.read(file);
            return result;
        }
    }


}
