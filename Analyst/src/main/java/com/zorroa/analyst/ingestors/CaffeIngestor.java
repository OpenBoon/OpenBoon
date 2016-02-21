package com.zorroa.analyst.ingestors;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.javacpp.opencv_core.Mat;

/**
 *
 * Uses Caffe to perform image classification
 *
 * @author wex
 *
 */
public class CaffeIngestor extends IngestProcessor {
    private static final Logger logger = LoggerFactory.getLogger(CaffeIngestor.class);

    @Argument
    private String deployFilename = "deploy.prototxt";

    @Argument
    private String caffeModelFilename = "bvlc_reference_caffenet.caffemodel";

    @Argument
    private String imagenetMeanFilename = "imagenet_mean.binaryproto";

    @Argument
    private String synsetWordFilename = "synset_words.txt";

    @Argument
    private CaffeClassifier caffeClassifier;

    @Override
    public void init() {
        String resourcePath = ModelUtils.modelPath() + "/caffe/imagenet/";
        logger.debug("Loading caffe models from " + resourcePath);
        String deployPath = new File(resourcePath + deployFilename).getAbsolutePath();
        String caffeModelPath = new File(resourcePath + caffeModelFilename).getAbsolutePath();
        String imagenetMeanPath = new File(resourcePath + imagenetMeanFilename).getAbsolutePath();
        String synsetWordPath = new File(resourcePath + synsetWordFilename).getAbsolutePath();
        try {
            caffeClassifier = new CaffeClassifier(deployPath, caffeModelPath, imagenetMeanPath, synsetWordPath);
            logger.debug("CaffeIngestor created");
        } catch (IOException e) {
            logger.error("Failed to initialize Caffe {}", e);
        }
    }

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
        BufferedImage image = ProxyUtils.getImage(227, asset);
        Mat mat = OpenCVUtils.convert(image);

        // Pass the image matrix to the classifier and get back an array of keywords+confidence
        final float confidenceThreshold = 0.1f;
        List<CaffeKeyword> caffeKeywords = caffeClassifier.classify(mat, 5, confidenceThreshold);

        // Add keywords with confidence and as a single block
        ArrayList<String> keywords = Lists.newArrayListWithCapacity(caffeKeywords.size());
        for (CaffeKeyword caffeKeyword : caffeKeywords) {
            keywords.addAll(caffeKeyword.keywords);
            asset.addKeywords(caffeKeyword.confidence, true /*suggest*/, caffeKeyword.keywords);
        }
        asset.setAttr("caffe", "keywords", keywords);
    }

    @Override
    public void teardown() {
        caffeClassifier.destroy();
    }
}
