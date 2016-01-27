package com.zorroa.ingestors;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.Proxy;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.ProxySchema;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import static org.opencv.highgui.Highgui.imread;

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
    private static OpenCVLoader openCVLoader = new OpenCVLoader();

    private float confidenceThreshold = 0.1f;

    // CaffeClassifier is not thread-safe, so give one to each thread
    private static final ThreadLocal<CaffeClassifier> caffeClassifier = new ThreadLocal<CaffeClassifier>() {
        @Override
        protected CaffeClassifier initialValue() {
            logger.debug("Loading caffe models...");
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
            logger.debug("CaffeIngestor created");
            return caffeClassifier;
        }
    };

    @Override
    public void process(AssetBuilder asset) {
        if (!asset.isSuperType("image")) {
            return;
        }

        ProxySchema proxyList = asset.getSchema("proxies", ProxySchema.class);
        if (proxyList == null) {
            logger.warn("Cannot find proxy list for {}, skipping Caffe analysis.", asset);
            return;
        }

        if (asset.contains("caffe")) {
            logger.debug("{} has already been processed by caffe.", asset);
            return;
        }

        String classifyPath = asset.getFile().getPath();
        for (Proxy proxy : proxyList) {
            if (proxy.getWidth() >= 256 || proxy.getHeight() >= 256) {
                classifyPath = proxy.getPath();
                break;
            }
        }

        // Read the image into a 3-channel BGR matrix.
        // Note that ImageIO.read returns a 4-channel ABGR
        Mat mat = imread(classifyPath);

        // Pass the image matrix to the classifier and get back an array of keywords+confidence
        CaffeKeyword[] caffeKeywords = caffeClassifier.get().classify(mat);

        // Convert the array of structs into an array of strings until we have a way
        // to pass the confidence values.
        ArrayList<String> keywords = Lists.newArrayListWithCapacity(caffeKeywords.length);
        for (int i = 0; i < caffeKeywords.length; ++i) {
            if (caffeKeywords[i].confidence > confidenceThreshold) {
                keywords.add(caffeKeywords[i].keyword);
                int confidence = Math.round(caffeKeywords[i].confidence * 6);
                confidence = confidence > 5 ? 5 : (confidence < 0 ? 0 : confidence);
                asset.addKeywords(confidence, true, caffeKeywords[i].keyword);
            }
        }
        asset.setAttr("caffe", "keywords", keywords);
    }

    @Override
    public void teardown() {
        caffeClassifier.get().destroy();
        caffeClassifier.set(null);
        caffeClassifier.remove();
    }
}
