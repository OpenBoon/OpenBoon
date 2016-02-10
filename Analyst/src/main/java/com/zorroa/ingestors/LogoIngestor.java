package com.zorroa.ingestors;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.util.StringUtil;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;



/**
 *
 * Uses openCV to detect logos
 *
 * @author jbuhler
 *
 */
public class LogoIngestor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(LogoIngestor.class);

    private static String cascadeName = "visaLogo.xml";

    // CascadeClassifier is not thread-safe, so give one to each thread
    private static final ThreadLocal<CascadeClassifier> cascadeClassifier = new ThreadLocal<CascadeClassifier>() {
        @Override
        protected CascadeClassifier initialValue() {
            Map<String, String> env = System.getenv();
            String modelPath = env.get("ZORROA_MODEL_PATH");
            if (modelPath == null) {
                logger.error("LogoIngestor requires ZORROA_MODEL_PATH");
                return null;
            }

            String haarPath = modelPath + "/logo/" + cascadeName;
            logger.debug("Logo processor haarPath path: {}", haarPath);
            CascadeClassifier classifier = null;
            try {
                classifier = new CascadeClassifier(haarPath);
                if (classifier != null) {
                    logger.debug("Logo classifier initialized");
                }
            } catch (Exception e) {
                logger.error("Logo classifier failed to initialize: " + e.toString());
            } finally {
                return classifier;
            }
        }
    };

    public LogoIngestor() { }

    @Override
    public void process(AssetBuilder asset) {

        if (!asset.isSuperType("image")) {
            return;
        }

        if (asset.contains("Logos")) {
            logger.debug("{} has already been processed by LogoIngestor.", asset);
            return;
        }

        String argCascadeName = (String) getArgs().get("CascadeName");
        if (argCascadeName != null) {
            cascadeName = argCascadeName;
        }
        if (!asset.getSource().getType().startsWith("image")) {
            return;
        }

        String classifyPath = asset.getFile().getPath();
        Size minLogo = new Size(100, 50);
        Size maxLogo = new Size(4000, 2000);

        // Perform analysis on the full resolution source image. The VISA and other logos tend to be rather small in
        // the frame, and using the proxy misses many logos.
        logger.debug("Starting logo detection on {}", asset.getFilename());
        Mat image = Highgui.imread(classifyPath);
        Size imSize = image.size();

        // The OpenCV levelWeights thing doesn't seem to work. We'll do a few calls to the detector with different thresholds
        // in order to estimate a confidence value.
        // In the code below, detectMultiScale is called four times, each with a bigger threshold for the number of detections required
        // (and also with a different scaling factor--these values were found by trial and error)
        // Confidence values are assigned depending on when the classifier finds the logo. We use a maximum value of 0.5,
        // in order to save the rest of the range (up to 1.0) to increase the confidence according to logo size.
        // OPTIMIZE: Use the size of the resulting rectangles to tweak minLogo and maxLogo in order to save the detector a bunch of work
        double confidence = 0;
        MatOfRect logoDetections = new MatOfRect();
        MatOfRect newLogoDetections = new MatOfRect();

        class ClassifyOptions {
            public double scaleFactor, detectionThreshold, finalConfidence;

            public ClassifyOptions(double scaleFactor, double detectionThreshold, double finalConfidence) {
                this.scaleFactor = scaleFactor;
                this.detectionThreshold = detectionThreshold;
                this.finalConfidence = finalConfidence;
            }
        }
        List<ClassifyOptions> options = Arrays.asList(
                new ClassifyOptions(1.0075, 20, 0.1),
                new ClassifyOptions(1.0075, 30, 0.2),
                new ClassifyOptions(1.005, 40, 0.3),
                new ClassifyOptions(1.005, 60, 0.5));

        int logoCount = 0;
        for (int i = 0; i < options.size(); i++) {
            cascadeClassifier.get().detectMultiScale(image, newLogoDetections, options.get(i).scaleFactor, (int)options.get(i).detectionThreshold, 0, minLogo, maxLogo);
            int count = newLogoDetections.toArray().length;
            if (count > 0) {
                confidence = options.get(i).finalConfidence;
                // I want to save Count and logoDetections ONLY if something was detected, so it's not overwritten to empty the last time
                logoCount = count;
                logoDetections = newLogoDetections;
            } else {
                break;
            }
        }

        if (logoCount < 1) {
            return;
        }

        logger.debug("LogoIngestor: Haar detected {} potential logos.", logoCount);
        List<String> keywords = Lists.newArrayList("visa");

        StringBuilder svgVal = new StringBuilder(1024);
        svgVal.append("<svg>");

        for (Rect rect : logoDetections.toArray()) {
            // Detect points in the area found by the Haar cascade
            int xmin = rect.x;
            if (xmin < 0) xmin = 0;

            int xmax = rect.x + rect.width;
            if (xmax >= imSize.width) xmax = (int)imSize.width-1;

            int ymin = rect.y;
            if (ymin < 0) ymin = 0;

            int ymax = rect.y + rect.height;
            if (ymax >= imSize.height) ymax = (int)imSize.height-1;

            // Logo is big if more than 5% of total height
            double relSize = rect.height / imSize.height;
            if (relSize > .05) {
                confidence += relSize;
                if (confidence > 1) {
                    confidence = 1;
                }
                keywords.add("bigvisa");
            }
            svgVal.append("<polygon points=\"");
            svgVal.append(StringUtil.join(",", xmin, ymin, xmax, ymin, xmax, ymax, xmin, ymax));
            svgVal.append("\" style=\"fill:none;stroke:green;stroke-width:2\" />");
        }

        logger.debug("LogoIngestor keywords {}", keywords);
        asset.addKeywords(confidence, true, keywords);

        // For debugging purposes, We are adding "visa"+confidence as an attribute, so we can see the actual number in
        // Curator and see how the sorting is working, what the bad outliers (false positives, false negatives) are,
        // and possibly tweak the confidence values we're assigning. Expect this to go away once we learn the values!
        // Note we didn't add this value to the keywords above, in order to avoid having the clumsy keyword used for search.
        keywords.add("visa" + confidence);
        asset.setAttr("Logos", "keywords", keywords);

        if (svgVal.length() > "<svg>".length()) {
            svgVal.append("</svg>");
            asset.setAttr("SVG", "Logos", svgVal.toString());
        }
    }
}

