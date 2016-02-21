package com.zorroa.analyst.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.Proxy;
import com.zorroa.archivist.sdk.schema.ProxySchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;

/**
 * Created by wex on 2/20/16.
 */
public final class ProxyUtils {
    private static final Logger logger = LoggerFactory.getLogger(ProxyUtils.class);

    private ProxyUtils() {}     // Disallow instantiation

    // Return the smallest proxy, or the source image, larger than the threshold
    public static BufferedImage getImage(int minDim, AssetBuilder asset) {
        ProxySchema proxyList = asset.getAttr("proxies");
        if (proxyList == null) {
            logger.warn("Cannot find proxy list for {}, skipping Caffe analysis.", asset);
            return null;
        }

        for (Proxy proxy : proxyList) {
            if (proxy.getWidth() >= minDim || proxy.getHeight() >= minDim) {
                return proxy.getImage();
            }
        }

        return asset.getImage();
    }
}
