package com.zorroa.archivist.processors;

import java.awt.Dimension;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zorroa.archivist.domain.AssetBuilder;
import com.zorroa.archivist.domain.Proxy;

/**
 *
 * Uses Caffe to perform image classification
 *
 * @author wex
 *
 */
public class CaffeProcessor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CaffeProcessor.class);

    static {
        // Note: Must use java -Djava.library.path=<path-to-jnilib>
        System.loadLibrary("CaffeProcessor");
    }

    public final native long createCaffeClassifier(String deployFile, String modelFile, String meanFile, String wordFile);
    public final native String classify(long caffeClassifier, String imageFile);

    long nativeCaffeClassifier;

    public CaffeProcessor() {
        // FIXME: Move network files into JAR bundle and use local references?
        String resourcePath = "/Users/wex/Zorroa/src/Archivist/src/main/resources/caffe/";
        nativeCaffeClassifier = createCaffeClassifier(
                resourcePath + "deploy.prototxt",
                resourcePath + "bvlc_reference_caffenet.caffemodel",
                resourcePath + "imagenet_mean.binaryproto",
                resourcePath + "synset_words.txt");
        logger.info("CaffeProcessor created");
    }

    @Override
    public void process(AssetBuilder asset) {
        if (isImageType(asset)) {
            // Start with the original image, but try to find a proxy to use for classification
            String classifyPath = asset.getFile().getPath();
            List<Proxy> proxyList = (List<Proxy>) asset.document.get("proxies");
            if (proxyList == null) {
                logger.error("Cannot find proxy list");
            } else {
                for (Proxy proxy : proxyList) {
                    if (proxy.getWidth() >= 256 || proxy.getHeight() >= 256) {
                        String proxyName = proxy.getFile();
                        proxyName = proxyName.substring(0, proxyName.lastIndexOf('.'));
                        classifyPath = imageService.generateProxyPath(proxyName, "png").getPath();
                        break;
                    }
                }
            }
            String keywords = classify(nativeCaffeClassifier, classifyPath);
            logger.info("CaffeProcessor " + keywords);
            List<String> keywordList = Arrays.asList(keywords.split(","));
            asset.put("caffe", "keywords", keywordList);
        }
    }

    public boolean isImageType(AssetBuilder asset) {
        return imageService.getSupportedFormats().contains(asset.getExtension());
    }
}
