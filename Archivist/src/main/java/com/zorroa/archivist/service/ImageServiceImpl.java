package com.zorroa.archivist.service;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.resizers.configurations.Rendering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.domain.Proxy;
import com.zorroa.archivist.domain.ProxyConfig;
import com.zorroa.archivist.domain.ProxyOutput;
import com.zorroa.archivist.repository.ProxyConfigDao;

@Service
public class ImageServiceImpl implements ImageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageServiceImpl.class);

    @Value("${archivist.proxies.basePath}")
    private String basePath;

    private File proxyPath;

    private ImmutableSet<String> supportedFormats;

    @Autowired
    ProxyConfigDao proxyConfigDao;

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
    public File generateProxyPath(String id, String format) {

        if (id.length() < PROXY_ID_MIN_LENGTH) {
            throw new RuntimeException("Proxy IDs need to be at leaset "
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

        Thumbnails.of(original)
            .width(output.getSize())
            .outputFormat(output.getFormat())
            .keepAspectRatio(true)
            .rendering(Rendering.QUALITY)
            .toFile(outFile);
        Dimension dim = getImageDimensions(outFile);

        Proxy result = new Proxy();
        result.setFile(outFile.getName());
        result.setWidth(dim.width);
        result.setHeight(dim.height);
        result.setFormat(output.getFormat());
        return result;
    }

    @Override
    public Set<String> getSupportedFormats() {
        return supportedFormats;
    }

    @Override
    public Dimension getImageDimensions(File imgFile) throws IOException {
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

    @Override
    public List<ProxyConfig> getProxyConfigs() {
        return proxyConfigDao.getAll();
    }

    @Override
    public ProxyConfig getProxyConfig(String id) {
        return proxyConfigDao.get(id);
    }
}
