package com.zorroa.archivist.web;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.ByteStreams;
import com.zorroa.archivist.sdk.filesystem.ObjectFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.File;
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
public class ProxyController {

    private static final Logger logger = LoggerFactory.getLogger(ProxyController.class);

    private static final int CACHE_MAX_SIZE = 5000;
    private static final int CACHE_TIMEOUT_MINUTES = 5;

    @Autowired
    ObjectFileSystem objectFileSystem;

    private final LoadingCache<String, ProxyImage> proxyCache = CacheBuilder.newBuilder()
        .maximumSize(CACHE_MAX_SIZE)
        .concurrencyLevel(20)
        .expireAfterWrite(CACHE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        .build(new CacheLoader<String, ProxyImage>() {
         public ProxyImage load(String key) throws Exception {
             return loadProxyImage(key);
         }
    });

    @RequestMapping(value = "/api/v1/proxy/image/{id:.+}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<byte[]> getProxy(@PathVariable String id) throws ExecutionException {
        ProxyImage image = proxyCache.get(id);
        return ResponseEntity.ok()
                .contentLength(image.size)
                .contentType(image.type)
                .body(image.content);
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

    private ProxyImage loadProxyImage(String id) throws IOException {
        return ProxyImage.load(objectFileSystem.find("proxies", id));
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
