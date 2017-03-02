package com.zorroa.archivist.service;

import com.zorroa.sdk.domain.Proxy;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Created by chambers on 7/8/16.
 */
public interface ImageService {
    ResponseEntity<InputStreamResource> serveImage(File file) throws IOException;

    ResponseEntity<InputStreamResource> serveImage(Proxy proxy) throws IOException;

    ByteArrayOutputStream watermark(File file, String format) throws IOException;

    BufferedImage watermark(BufferedImage src);
}
