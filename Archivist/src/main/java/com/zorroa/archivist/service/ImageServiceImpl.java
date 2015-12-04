package com.zorroa.archivist.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.sdk.domain.Proxy;
import com.zorroa.archivist.sdk.domain.ProxyOutput;
import com.zorroa.archivist.sdk.service.ImageService;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.resizers.configurations.Rendering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class ImageServiceImpl implements ImageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageServiceImpl.class);

    @Value("${archivist.proxies.basePath}")
    private String basePath;

    @Value("${archivist.proxies.format}")
    private String defaultProxyFormat;

    private File proxyPath;

    private ImmutableSet<String> supportedFormats;

    private static final LoadingCache<File, BufferedImage> IMAGE_CACHE = CacheBuilder.newBuilder()
            .maximumSize(200)
            .initialCapacity(200)
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .concurrencyLevel(4)
            .build(new CacheLoader<File, BufferedImage>() {
                public BufferedImage load(File key) throws Exception {
                    return ImageIO.read(key);
                }
            });

    @PostConstruct
    public void init() {
        proxyPath = new File(basePath);
        proxyPath.mkdirs();

        ImmutableSet.Builder<String> builder = ImmutableSet.<String>builder();
        for (String name: ImageIO.getReaderFormatNames()) {
            builder.add(name);
        }
        supportedFormats = builder.build();
    }

    /**
     * In case we ever move to an different ID generation scheme, make
     * sure we have enough characters to build the directory structure
     * and avoid collisions.
     */
    private static final int PROXY_ID_MIN_LENGTH = 16;

    @Override
    public File generateProxyPath(String name) {
        String[] e = name.split("\\.");
        return generateProxyPath(e[0], e[1]);
    }

    @Override
    public File generateProxyPath(String id, String format) {

        if (id.length() < PROXY_ID_MIN_LENGTH) {
            throw new RuntimeException("Proxy IDs need to be at least "
                    + PROXY_ID_MIN_LENGTH + " characters.");
        }

        StringBuilder sb = new StringBuilder(512);
        sb.append(proxyPath.getAbsolutePath());
        sb.append("/");

        for (int i=0; i<=4; i++) {
            sb.append(id.charAt(i));
            sb.append("/");
        }
        sb.append(id);
        sb.append("." + format);
        return new File(sb.toString());
    }

    public File makeProxyPath(String id, String format) {
        File newPath = generateProxyPath(id, format);
        newPath.getParentFile().mkdirs();
        return newPath;
    }

    @Override
    public Proxy makeProxy(File original, ProxyOutput output) throws IOException {
        String proxyId = UUID.randomUUID().toString();
        File outFile = makeProxyPath(proxyId, output.getFormat());

        BufferedImage proxy = Thumbnails.of(getImage(original))
                .width(output.getSize())
                .outputFormat(output.getFormat())
                .keepAspectRatio(true)
                .rendering(Rendering.QUALITY)
                .outputQuality(output.getQuality())
                .asBufferedImage();
        ImageIO.write(proxy, output.getFormat(), outFile);

        Proxy result = new Proxy();
        result.setPath(outFile.getAbsolutePath());
        result.setWidth(proxy.getWidth());
        result.setHeight(proxy.getHeight());
        result.setFormat(output.getFormat());
        return result;

    }

    @Override
    public Set<String> getSupportedFormats() {
        return supportedFormats;
    }

    @Override
    public String getDefaultProxyFormat() {
        return defaultProxyFormat;
    }

    @Override
    public Dimension getImageDimensions(File file) throws IOException {
        BufferedImage img = getImage(file);
        return new Dimension(img.getWidth(), img.getHeight());
    }

    private BufferedImage getImage(File file)  throws IOException {
        try {
            return IMAGE_CACHE.get(file);
        } catch (ExecutionException e) {
            throw new IOException(e.getCause());
        }
    }

}
