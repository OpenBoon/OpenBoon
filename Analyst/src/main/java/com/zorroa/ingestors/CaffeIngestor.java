package com.zorroa.ingestors;

import java.io.File;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

import com.zorroa.archivist.sdk.IngestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zorroa.archivist.sdk.AssetBuilder;
import com.zorroa.archivist.sdk.Proxy;

/**
 *
 * Uses Caffe to perform image classification
 *
 * @author wex
 *
 */
public class CaffeIngestor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CaffeIngestor.class);

    static {
        // Note: Must use java -Djava.library.path=<path-to-jnilib>
        System.loadLibrary("CaffeIngestor");
    }

    public final native long createCaffeClassifier(String deployFile, String modelFile, String meanFile, String wordFile);
    public final native String classify(long caffeClassifier, String imageFile);

    long nativeCaffeClassifier;

    public CaffeIngestor() {
        Map<String, String> env = System.getenv();
        String modelPath = env.get("ZORROA_OPENCV_MODEL_PATH");
        if (modelPath == null) {
            logger.error("CaffeIngestor requires ZORROA_OPENCV_MODEL_PATH");
            return;
        }
        String resourcePath = modelPath + "/caffe/imagenet/";
        String[] name = { "deploy.prototxt", "bvlc_reference_caffenet.caffemodel",
                "imagenet_mean.binaryproto", "synset_words.txt" };
        File[] file = new File[4];
        for (int i = 0; i < 4; ++i) {
            file[i] = new File(resourcePath + name[i]);
            if (!file[i].exists()) {
                logger.error("CaffeIngestor model file " + file[i].getAbsolutePath() + " does not exist");
                return;
            }
        }
        nativeCaffeClassifier = createCaffeClassifier(file[0].getAbsolutePath(),
                file[1].getAbsolutePath(), file[2].getAbsolutePath(), file[3].getAbsolutePath());
        logger.info("CaffeIngestor created");
    }

    @Override
    public void process(AssetBuilder asset) {
        if (ingestProcessorService.isImage(asset)) {
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
                        classifyPath = ingestProcessorService.getProxyFile(proxyName, "png").getPath();
                        break;
                    }
                }
            }
            String keywords = classify(nativeCaffeClassifier, classifyPath);
            logger.info("CaffeIngestor: " + keywords);
            List<String> keywordList = Arrays.asList(keywords.split(","));
            asset.map("caffe", "keywords", "type", "string");
            asset.map("caffe", "keywords", "copy_to", null);
            asset.put("caffe", "keywords", keywordList);
        }
    }
}
