package com.zorroa.analyst.ingestors;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.Proxy;
import com.zorroa.archivist.sdk.exception.IngestException;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.Argument;
import com.zorroa.archivist.sdk.schema.ProxySchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private CaffeClassifier caffeClassifier;

    @Override
    public void init() {
        String resourcePath = applicationProperties.getString("analyst.path.models") + "/caffe/imagenet/";
        logger.debug("Loading caffe models from {}",resourcePath);
        File deployFile = new File(resourcePath + deployFilename);
        if (!deployFile.exists() || !deployFile.canRead()) {
            logger.error("Cannot load caffe model {}", deployFile);
            throw new IngestException("Cannot load caffe model " + deployFile);
        }
        File caffeModelFile = new File(resourcePath + caffeModelFilename);
        if (!caffeModelFile.exists() || !caffeModelFile.canRead()) {
            logger.error("Cannot load caffe model {}", caffeModelFile);
            throw new IngestException("Cannot load caffe model " + caffeModelFile);
        }
        File imagenetMeanFile = new File(resourcePath + imagenetMeanFilename);
        if (!imagenetMeanFile.exists() || !imagenetMeanFile.canRead()) {
            logger.error("Cannot load caffe model {}", imagenetMeanFile);
            throw new IngestException("Cannot load caffe model " + imagenetMeanFilename);
        }
        File synsetWordFile = new File(resourcePath + synsetWordFilename);
        if (!synsetWordFile.exists() || !synsetWordFile.canRead()) {
            logger.error("Cannot load caffe model {}", synsetWordFile);
            throw new IngestException("Cannot load caffe model " + synsetWordFilename);
        }
        try {
            caffeClassifier = new CaffeClassifier(deployFile.getAbsolutePath(),caffeModelFile.getAbsolutePath(),
                    imagenetMeanFile.getAbsolutePath(), synsetWordFile.getAbsolutePath());
            logger.debug("CaffeIngestor created");
        } catch (IOException e) {
            logger.error("Failed to initialize Caffe", e);
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

        Proxy proxy = asset.getSchema("proxies", ProxySchema.class).atLeastThisSize(227);
        if (proxy == null) {
            logger.warn("Skipping CaffeIngestor, no proxy of suitable size. (227)");
            return;
        }

        Mat mat = OpenCVUtils.convert(proxy.getImage());

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
