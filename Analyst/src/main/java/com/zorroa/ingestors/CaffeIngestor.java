package com.zorroa.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.domain.Proxy;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 *
 * Uses Caffe to perform image classification
 *
 * @author wex
 *
 */
public class CaffeIngestor extends IngestProcessor {
    private static final Logger logger = LoggerFactory.getLogger(CaffeIngestor.class);

    private static CaffeLoader caffeLoader = new CaffeLoader();

    // CaffeClassifier is not thread-safe, so give one to each thread
    private static final ThreadLocal<CaffeClassifier> caffeClassifier = new ThreadLocal<CaffeClassifier>() {
        @Override
        protected CaffeClassifier initialValue() {
            logger.info("Loading caffe models...");
            Map<String, String> env = System.getenv();
            String modelPath = env.get("ZORROA_OPENCV_MODEL_PATH");
            if (modelPath == null) {
                logger.error("CaffeIngestor requires ZORROA_OPENCV_MODEL_PATH");
                return null;
            }
            String resourcePath = modelPath + "/caffe/imagenet/";
            String[] name = {"deploy.prototxt", "bvlc_reference_caffenet.caffemodel",
                    "imagenet_mean.binaryproto", "synset_words.txt"};
            File[] file = new File[4];
            for (int i = 0; i < 4; ++i) {
                file[i] = new File(resourcePath + name[i]);
                if (!file[i].exists()) {
                    logger.error("CaffeIngestor model file " + file[i].getAbsolutePath() + " does not exist");
                    return null;
                }
            }
            CaffeClassifier caffeClassifier = new CaffeClassifier(file[0].getAbsolutePath(),
                    file[1].getAbsolutePath(), file[2].getAbsolutePath(), file[3].getAbsolutePath());
            logger.info("CaffeIngestor created");
            return caffeClassifier;
        }
    };

    @Override
    public void process(AssetBuilder asset) {
        if (!asset.isImage()) {
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
                classifyPath = proxy.getPath();
                break;
            }
        }

        // Perform Caffe analysis
        File file = new File(classifyPath);
        BufferedImage image = null;
        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Mat mat = BufferedImageMat.convertBufferedImageToMat(image);
        CaffeKeyword[] caffeKeywords = caffeClassifier.get().classify(mat);

        String[] keywords = new String[caffeKeywords.length];
        for (int i = 0; i < caffeKeywords.length; ++i) {
            keywords[i] = caffeKeywords[i].keyword;
        }
        logger.info("CaffeIngestor: " + Arrays.toString(keywords));
        asset.putKeywords("caffe", "keywords", (String[]) keywords);
    }

    @Override
    public void teardown() {
        caffeClassifier.get().destroy();
        caffeClassifier.set(null);
        caffeClassifier.remove();
    }
}