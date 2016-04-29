package com.zorroa.analyst.web;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.ByteStreams;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.filesystem.ObjectFileSystem;
import com.zorroa.sdk.util.FileUtils;
import com.zorroa.common.repository.AssetDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Provides endpoints for downloading proxy images.
 *
 * @author chambers
 *
 */
@Controller
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

    /**
     * The proxies URL is broken out rather than using the general purpose getFile method because
     * proxies are handled differently due to caching.
     *
     * @param request
     * @param response
     * @return
     * @throws ExecutionException
     */
    @RequestMapping(value = "/api/v1/fs/proxies/**", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<byte[]> getProxy(HttpServletRequest request, HttpServletResponse response) throws ExecutionException, FileNotFoundException {
        response.setHeader("Cache-Control", "public");

        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        AntPathMatcher apm = new AntPathMatcher();
        String finalPath = "proxies/" + FileUtils.filename(apm.extractPathWithinPattern(bestMatchPattern, path));

        ProxyImage image = proxyCache.get(finalPath);
        return ResponseEntity.ok()
                .contentLength(image.size)
                .contentType(image.type)
                .body(image.content);
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
                .contentType(MediaType.valueOf(asset.getSource().getType()))
                .contentLength(asset.getSource().getFileSize())
                .body(new FileSystemResource(asset.getSource().getPath()));
    }

    @ExceptionHandler(ExecutionException.class)
    public ResponseEntity<byte[]> brokenImage() throws IOException {
        byte[] bytes = ByteStreams.toByteArray(
                getClass().getResourceAsStream("/broken128.png"));
        return ResponseEntity.ok()
                .contentLength(bytes.length)
                .contentType(MediaType.IMAGE_PNG)
                .body(bytes);
    }

    private ProxyImage loadProxyImage(String path) throws IOException {
        return ProxyImage.load(objectFileSystem.get(path).getFile());
    }

    private static class ProxyImage {
        public byte[] content;
        public long size;
        public MediaType type;

        public static final ProxyImage load(File file) throws IOException {
            RandomAccessFile f = new RandomAccessFile(file, "r");
            try {
                ProxyImage result = new ProxyImage();
                result.content = new byte[(int)f.length()];
                f.readFully(result.content);
                result.size = f.length();
                result.type = MediaType.IMAGE_JPEG;
                return result;
            } finally {
                f.close();
            }
        }
    }
}
