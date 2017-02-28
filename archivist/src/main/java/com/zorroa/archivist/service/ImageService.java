package com.zorroa.archivist.service;

import com.zorroa.sdk.domain.Proxy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by chambers on 7/8/16.
 */
public interface ImageService {
    ByteArrayOutputStream watermark(Proxy proxy) throws IOException;

    BufferedImage watermark(BufferedImage src);
}
