package com.zorroa.analyst.ingestors;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.Ingest;
import com.zorroa.archivist.sdk.domain.Proxy;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.ProxySchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.bytedeco.javacpp.opencv_core.Mat;
import static org.bytedeco.javacpp.opencv_core.Rect;
import static org.bytedeco.javacpp.opencv_core.RectVector;
import static org.bytedeco.javacpp.opencv_core.Size;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;


/**
 *
 * Uses openCV to detect faces
 *
 * @author jbuhler
 *
 */
public class FaceIngestor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FaceIngestor.class);

    private static String cascadeName = "haarcascade_frontalface_alt.xml";

    // CascadeClassifier is not thread-safe, so give one to each thread
    private static final ThreadLocal<CascadeClassifier> cascadeClassifier = new ThreadLocal<CascadeClassifier>(){
        @Override
        protected CascadeClassifier initialValue() {
            Map<String, String> env = System.getenv();
            String modelPath = env.get("ZORROA_MODEL_PATH");
            if (modelPath == null) {
                logger.error("FaceIngestor requires ZORROA_MODEL_PATH");
                return null;
            }
            String haarPath = modelPath + "/face/" + cascadeName;
            CascadeClassifier classifier = null;
            try {
                classifier = new CascadeClassifier(haarPath);
                if (classifier != null) {
                    logger.debug("Face classifier initialized");
                }
            } catch (Exception e) {
                logger.error("Face classifier failed to initialize: " + e.toString());
            } finally {
                return classifier;
            }
        }
    };

    public FaceIngestor() { }

    @Override
    public void init() {
        String argCascadeName = (String) getArgs().get("CascadeName");
        if (argCascadeName != null) {
            cascadeName = argCascadeName;
        }
    }

    @Override
    public void process(AssetBuilder asset) {

        if (!asset.isSuperType("image")) {
            return;
        }

        if (asset.contains("face") && !asset.isChanged()) {
            logger.debug("{} has already been processed by FaceIngestor.", asset);
            return;
        }

        ProxySchema proxyList = asset.getSchema("proxies", ProxySchema.class);
        if (proxyList == null) {
            logger.warn("Cannot find proxy list for {}, skipping Face analysis.", asset);
            return;
        }

        String classifyPath = asset.getFile().getPath();
        Size minFace = new Size(80, 80);
        Size maxFace = new Size(1000, 1000);
        for (Proxy proxy : proxyList) {
            if (proxy.getWidth() >= 1000 || proxy.getHeight() >= 1000) {
                classifyPath = proxy.getPath();
                break;
            }
        }

        // Perform facial analysis
        logger.debug("Starting facial detection on {} ", asset);
        Mat image = imread(classifyPath);
        Size imSize = image.size();

        // The OpenCV levelWeights thing doesn't seem to work. We'll do a few calls to the detector with different thresholds
        // in order to estimate a confidence value
        // In the code below, detectMultiScale is called four times, each with a bigger threshold for the number of detections required
        // (and also with a different scaling factor--these values were found by trial and error)
        // Confidence values are assigned depending on when the classifier finds the face. We use a maximum value of 0.5,
        // in order to save the rest of the range (up to 1.0) to increase the confidence according to logo size.
        // OPTIMIZE: Use the size of the resulting rectangles to tweak minFace and maxFace in order to save the detector a bunch of work
        double confidence = 0;
        RectVector faceDetections = new RectVector();
        RectVector newFaceDetections = new RectVector();

        class ClassifyOptions {
            public double scaleFactor, detectionThreshold, finalConfidence;

            public ClassifyOptions(double scaleFactor, double detectionThreshold, double finalConfidence) {
                this.scaleFactor = scaleFactor;
                this.detectionThreshold = detectionThreshold;
                this.finalConfidence = finalConfidence;
            }
        }
        List<ClassifyOptions> options = Arrays.asList(
                new ClassifyOptions(1.05, 5, 0.1),
                new ClassifyOptions(1.05, 10, 0.2),
                new ClassifyOptions(1.05, 20, 0.3),
                new ClassifyOptions(1.05, 40, 0.5));

        long faceCount = 0;
        for (int i = 0; i < options.size(); i++) {
            cascadeClassifier.get().detectMultiScale(image, newFaceDetections, options.get(i).scaleFactor, (int)options.get(i).detectionThreshold, 0, minFace, maxFace);
            long count = newFaceDetections.size();
            if (count > 0) {
                confidence = options.get(i).finalConfidence;
                // We want to save Count and faceDetections ONLY if something was detected, so it's not overwritten to empty the last time
                faceCount = count;
                faceDetections = newFaceDetections;
            } else {
                break;
            }
        }

        logger.debug("Detected '{}' faces in {}", faceCount, asset);
        if (faceCount > 0) {
            List<String> keywords = Lists.newArrayList("face", "face" + faceCount);

            // Detect faces that are big enough for the 'bigface' label
            for (int i = 0; i < faceDetections.size(); ++i) {
                Rect rect = faceDetections.get(i);
                double relSize = rect.height() / imSize.height();
                if (relSize > .1) {
                    confidence += relSize;
                    if (confidence > 1) {
                        confidence = 1;
                    }
                    keywords.add("bigface");
                }
            }
            asset.addKeywords(confidence, true, keywords);

            // For debugging purposes, We are adding "face"+confidence as an attribute, so we can see the actual number in
            // Curator and see how the sorting is working, what the bad outliers (false positives, false negatives) are,
            // and possibly tweak the confidence values we're assigning. Expect this to go away once we learn the values!
            // Note we didn't add this value to the keywords above, in order to avoid having the clumsy keyword used for search.
            keywords.add("face" + confidence);
            asset.setAttr("face", "keywords", keywords);
        }
    }
}
