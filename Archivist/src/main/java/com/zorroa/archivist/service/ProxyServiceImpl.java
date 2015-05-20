package com.zorroa.archivist.service;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.filters.Canvas;
import net.coobird.thumbnailator.geometry.Positions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.zorroa.archivist.IngestProxyException;
import com.zorroa.archivist.domain.Proxy;

@Service
public class ProxyServiceImpl implements ProxyService {

    private static final Logger logger = LoggerFactory.getLogger(ProxyServiceImpl.class);

    @Value("${archivist.proxies.path}")
    private String basePath;

    @Value("${archivist.proxies.format}")
    private String format;

    private File proxyPath;

    @PostConstruct
    public void init() {
        proxyPath = new File(basePath);
        proxyPath.mkdirs();
    }

    public File makeProxyPath() {
        String id = UUID.randomUUID().toString();

        StringBuilder sb = new StringBuilder(512);
        sb.append(proxyPath.getAbsolutePath());
        sb.append("/");

        for (int i=0; i<=4; i++) {
            sb.append(id.charAt(i));
            sb.append("/");
        }
        sb.append(id);
        sb.append("." + format);

        File newPath = new File(sb.toString());
        newPath.getParentFile().mkdirs();
        return newPath;
    }

    @Override
    public Proxy makeProxy(File original, double scale) {
        File outFile = makeProxyPath();
        try {
            Thumbnails.of(original).scale(scale).toFile(outFile);
            Dimension dim = getImageDimension(outFile);

            Proxy result = new Proxy();
            result.setPath(outFile.getAbsolutePath());
            result.setWidth(dim.width);
            result.setHeight(dim.height);
            return result;

        } catch (IOException e) {
            throw new IngestProxyException("Failed to write proxy image: " + e, e);
        }
    }

    @Override
    public Proxy makeProxy(File original, int width, int height) {
        File outFile = makeProxyPath();
        try {
            Thumbnails.of(original)
                .size(width, height)
                .addFilter(new Canvas(width, height, Positions.CENTER, Color.BLACK))
                .toFile(outFile);

            Proxy result = new Proxy();
            result.setPath(outFile.getAbsolutePath());
            result.setWidth(width);
            result.setHeight(height);
            return result;

        } catch (IOException e) {
            throw new IngestProxyException("Failed to write proxy image: " + e, e);
        }
    }

    public static Dimension getImageDimension(File imgFile) throws IOException {
        int pos = imgFile.getName().lastIndexOf(".");
        if (pos == -1)
            throw new IOException("No extension for file: " + imgFile.getAbsolutePath());
        String suffix = imgFile.getName().substring(pos + 1);
        Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
        if (iter.hasNext()) {
            ImageReader reader = iter.next();
            try {
                ImageInputStream stream = new FileImageInputStream(imgFile);
                reader.setInput(stream);
                int width = reader.getWidth(reader.getMinIndex());
                int height = reader.getHeight(reader.getMinIndex());
                return new Dimension(width, height);
            } catch (IOException e) {

            } finally {
                reader.dispose();
            }
        }

        throw new IOException("Not a known image file: " + imgFile.getAbsolutePath());
    }
}
