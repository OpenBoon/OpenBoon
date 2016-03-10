package com.zorroa.analyst.web;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.ByteStreams;
import com.zorroa.archivist.sdk.filesystem.ObjectFile;
import com.zorroa.archivist.sdk.filesystem.ObjectFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
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

    private static final int CACHE_MAX_SIZE = 5000;
    private static final int CACHE_TIMEOUT_MINUTES = 5;

    private final LoadingCache<String, ProxyImage> proxyCache = CacheBuilder.newBuilder()
        .maximumSize(CACHE_MAX_SIZE)
        .concurrencyLevel(20)
        .expireAfterWrite(CACHE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        .build(new CacheLoader<String, ProxyImage>() {
         public ProxyImage load(String key) throws Exception {
             return loadProxyImage(key);
         }
    });

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
        String finalPath = "proxies/" + apm.extractPathWithinPattern(bestMatchPattern, path);

        ProxyImage image = proxyCache.get(finalPath);
        return ResponseEntity.ok()
                .contentLength(image.size)
                .contentType(image.type)
                .body(image.content);
    }

    /**
     * This endpoint can stream any type of data in the Analyst's object storage.
     *
     * @param request
     * @param response
     * @return
     * @throws ExecutionException
     * @throws FileNotFoundException
     */
    @RequestMapping(value = "/api/v1/fs/**", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<FileSystemResource> getFile(HttpServletRequest request, HttpServletResponse response) throws ExecutionException, FileNotFoundException {

        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        AntPathMatcher apm = new AntPathMatcher();
        String finalPath = apm.extractPathWithinPattern(bestMatchPattern, path);

        ObjectFile file = objectFileSystem.get(finalPath);
        return ResponseEntity.ok()
                .contentLength(file.size())
                .body(new FileSystemResource(file.getFile()));
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
