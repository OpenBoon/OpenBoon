package com.zorroa.ingestors;

import com.zorroa.archivist.sdk.AssetBuilder;
import com.zorroa.archivist.sdk.IngestProcessor;
import com.zorroa.archivist.sdk.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    public static final native long createCaffeClassifier(String deployFile, String modelFile, String meanFile, String wordFile);
    public static final native String classify(long caffeClassifier, String imageFile);
    public static final native void destroyCaffeClassifier(long classifier);

    // CaffeClassifier is not thread-safe, so give one to each thread
    private static final ThreadLocal<Long> caffeClassifier = new ThreadLocal<Long>() {
        @Override
        protected Long initialValue() {
            Map<String, String> env = System.getenv();
            String modelPath = env.get("ZORROA_OPENCV_MODEL_PATH");
            if (modelPath == null) {
                logger.error("CaffeIngestor requires ZORROA_OPENCV_MODEL_PATH");
                return Long.valueOf(0);
            }
            String resourcePath = modelPath + "/caffe/imagenet/";
            String[] name = { "deploy.prototxt", "bvlc_reference_caffenet.caffemodel",
                    "imagenet_mean.binaryproto", "synset_words.txt" };
            File[] file = new File[4];
            for (int i = 0; i < 4; ++i) {
                file[i] = new File(resourcePath + name[i]);
                if (!file[i].exists()) {
                    logger.error("CaffeIngestor model file " + file[i].getAbsolutePath() + " does not exist");
                    return Long.valueOf(0);
                }
            }
            long nativeCaffeClassifier = createCaffeClassifier(file[0].getAbsolutePath(),
                    file[1].getAbsolutePath(), file[2].getAbsolutePath(), file[3].getAbsolutePath());
            logger.info("CaffeIngestor created");
            return Long.valueOf(nativeCaffeClassifier);
        }
    };


    public CaffeIngestor() { }

    @Override
    public void process(AssetBuilder asset) {
        if (!ingestProcessorService.isImage(asset)) {
            return;     // Only process images
        }

        List<Proxy> proxyList = (List<Proxy>) asset.getDocument().get("proxies");
        if (proxyList == null) {
            logger.error("Cannot find proxy list for " + asset.getFilename() + ", skipping Caffe analysis.");
            return;     // No proxies implies bad image file, which crashes Caffe
        }

        String classifyPath = asset.getFile().getPath();
        for (Proxy proxy : proxyList) {
            if (proxy.getWidth() >= 256 || proxy.getHeight() >= 256) {
                String proxyName = proxy.getFile();
                proxyName = proxyName.substring(0, proxyName.lastIndexOf('.'));
                classifyPath = ingestProcessorService.getProxyFile(proxyName, "png").getPath();
                break;
            }
        }

        // Perform Caffe analysis
        long nativeCaffeClassifier = caffeClassifier.get().longValue();
        String value = classify(nativeCaffeClassifier, classifyPath);
        logger.info("CaffeIngestor: " + value);
        String[] keywords = (String[]) Arrays.asList(value.split(",")).toArray();
        asset.putKeywords("caffe", "keywords", (String[]) keywords);
    }

    @Override
    public void teardown() {
        long nativeCaffeClassifier = caffeClassifier.get().longValue();
        if (nativeCaffeClassifier != 0) {
            destroyCaffeClassifier(nativeCaffeClassifier);
        }
        caffeClassifier.set(null);
        caffeClassifier.remove();
    }

    protected void finalize() throws Throwable {
        super.finalize();
        logger.info("Caffe finalizer invoked.");
    }
}
