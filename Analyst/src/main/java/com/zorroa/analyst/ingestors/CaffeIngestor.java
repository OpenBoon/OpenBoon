package com.zorroa.analyst.ingestors;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.Proxy;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.ProxySchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

    // CaffeClassifier is not thread-safe, so give one to each thread
    private static final ThreadLocal<CaffeClassifier> caffeClassifier = new ThreadLocal<CaffeClassifier>() {
        @Override
        protected CaffeClassifier initialValue() {
            logger.debug("Loading caffe models...");
            Map<String, String> env = System.getenv();
            String modelPath = env.get("ZORROA_MODEL_PATH");
            if (modelPath == null) {
                logger.error("CaffeIngestor requires ZORROA_MODEL_PATH");
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
            CaffeClassifier caffeClassifier = null;
            try {
                caffeClassifier = new CaffeClassifier(file[0].getAbsolutePath(),
                        file[1].getAbsolutePath(), file[2].getAbsolutePath(), file[3].getAbsolutePath());
            } catch (IOException e) {
                logger.error("Failed to initialize Caffe {}", e);
            }
            logger.debug("CaffeIngestor created");
            return caffeClassifier;
        }
    };

    @Override
    public void process(AssetBuilder asset) {
        if (!asset.isSuperType("image")) {
            return;
        }

        if (asset.contains("caffe") && !asset.isChanged()) {
            logger.debug("{} has already been processed by caffe.", asset);
            return;
        }

        // Get the source image as an OpenCV Mat
        BufferedImage source = selectSource(asset);

        // Pass the image matrix to the classifier and get back an array of keywords+confidence
        final float confidenceThreshold = 0.1f;
        List<CaffeKeyword> caffeKeywords = caffeClassifier.get().classify(source, 5, confidenceThreshold);

        // Add keywords with confidence and as a single block
        ArrayList<String> keywords = Lists.newArrayListWithCapacity(caffeKeywords.size());
        for (CaffeKeyword caffeKeyword : caffeKeywords) {
            keywords.addAll(caffeKeyword.keywords);
            asset.addKeywords(caffeKeyword.confidence, true /*suggest*/, caffeKeyword.keywords);
        }
        asset.setAttr("caffe", "keywords", keywords);
    }

    private BufferedImage selectSource(AssetBuilder asset) {
        ProxySchema proxyList = asset.getAttr("proxies");
        if (proxyList == null) {
            logger.warn("Cannot find proxy list for {}, skipping Caffe analysis.", asset);
            return null;
        }

        for (Proxy proxy : proxyList) {
            if (proxy.getWidth() >= 256 || proxy.getHeight() >= 256) {
                return proxy.getImage();
            }
        }

        return asset.getImage();
    }

    @Override
    public void teardown() {
        caffeClassifier.get().destroy();
        caffeClassifier.set(null);
        caffeClassifier.remove();
    }
}
