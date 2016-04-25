package com.zorroa.analyst.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.Proxy;
import com.zorroa.archivist.sdk.exception.IngestException;
import com.zorroa.archivist.sdk.processor.Argument;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.ProxySchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
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

        if (asset.attrExists("caffe") && !asset.isChanged()) {
            logger.debug("{} has already been processed by caffe.", asset);
            return;
        }

        ProxySchema proxies = asset.getAttr("proxies", ProxySchema.class);
        Proxy proxy = proxies.atLeastThisSize(227);
        BufferedImage img = null;

        if (proxy != null) {
            try {
                img = ImageIO.read(objectFileSystem.transfer(
                    URI.create(proxy.getUri()), objectFileSystem.get(proxy.getName())).getFile());
            } catch (IOException e) {
                logger.warn("Failed to find/transfer proxy image on this node.");
            }
        }

        if (img == null) {
            img = asset.getImage();
            if (img == null ) {
                logger.warn("Skipping CaffeIngestor, no proxy or source of suitable size. (227)");
                return;
            }
        }

        Mat mat = OpenCVUtils.convert(img);
        logger.debug("Starting caffe ingestion on {}", mat);

        // Pass the image matrix to the classifier and get back an array of keywords+confidence
        final float confidenceThreshold = 0.1f;
        List<CaffeKeyword> caffeKeywords = caffeClassifier.classify(mat, 5, confidenceThreshold);

        for (CaffeKeyword caffeKeyword : caffeKeywords) {
            /**
             * TODO: I've never seen a caffe confidence over 0.1
             * they are basically all the same confidence.
             *
             */
            if (caffeKeyword.confidence > 0.1f) {
                asset.addSuggestKeywords("caffe_high", caffeKeyword.keywords);
                asset.addKeywords("caffe", caffeKeyword.keywords);
            }
            else {
                asset.addSuggestKeywords("caffe_low", caffeKeyword.keywords);
                asset.addKeywords("caffe", caffeKeyword.keywords);
            }

        }
    }

    @Override
    public void teardown() {
        caffeClassifier.destroy();
    }
}
