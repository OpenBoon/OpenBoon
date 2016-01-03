package com.zorroa.archivist.service;

import com.zorroa.archivist.sdk.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;

@Service
public class ImageServiceImpl implements ImageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageServiceImpl.class);

    @Value("${archivist.proxies.basePath}")
    private String basePath;

    @Value("${archivist.proxies.format}")
    private String defaultProxyFormat;

    private File proxyPath;

    @PostConstruct
    public void init() {
        proxyPath = new File(basePath);
        proxyPath.mkdirs();
    }

    /**
     * In case we ever move to an different ID generation scheme, make
     * sure we have enough characters to build the directory structure
     * and avoid collisions.
     */
    private static final int PROXY_ID_MIN_LENGTH = 16;

    @Override
    public File allocateProxyPath(String id, String format) {

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
        File result = new File(sb.toString());
        result.getParentFile().mkdirs();
        return result;
    }

    @Override
    public String getDefaultProxyFormat() {
        return defaultProxyFormat;
    }
}
