package com.zorroa.analyst.ingestors;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.Proxy;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.Argument;
import com.zorroa.archivist.sdk.schema.ProxySchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.bytedeco.javacpp.opencv_core.*;
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

    @Argument
    private String cascadeName = "haarcascade_frontalface_alt.xml";

    private CascadeClassifier cascadeClassifier;

    @Override
    public void init() {
        String path = applicationProperties.getString("analyst.path.models") + "/face/" + cascadeName;
        cascadeClassifier = new CascadeClassifier(path);
        if (cascadeClassifier == null) {
            logger.debug("Face classifier failed to initialize");
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

        Mat image;

        Proxy proxy = asset.getSchema("proxies", ProxySchema.class).atLeastThisSize(1000);
        if (proxy != null) {
            image = OpenCVUtils.convert(proxy.getImage());
        }
        else {
            logger.debug("FaceIngestor cannot no proxy of suitable size, using source");
            image = OpenCVUtils.convert(asset.getImage());
        }

        logger.debug("Starting facial detection on {} ", asset);

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
            cascadeClassifier.detectMultiScale(image, newFaceDetections,
                    options.get(i).scaleFactor, (int)options.get(i).detectionThreshold,
                    0 /*flags*/, new Size(80, 80) /*min*/, new Size(1000, 1000) /*max*/);
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
                double relSize = rect.height() / image.size().height();
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
