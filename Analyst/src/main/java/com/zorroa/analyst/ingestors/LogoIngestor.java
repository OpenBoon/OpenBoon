package com.zorroa.analyst.ingestors;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.Argument;
import com.zorroa.archivist.sdk.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;


/**
 *
 * Uses openCV to detect logos
 *
 * @author jbuhler
 *
 */
public class LogoIngestor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(LogoIngestor.class);

    @Argument
    private String cascadeName = "visaLogo.xml";

    private CascadeClassifier cascadeClassifier;

    @Override
    public void init() {
        String path = applicationProperties.getString("analyst.path.models") + "/logo/" + cascadeName;
        cascadeClassifier = new CascadeClassifier(path);
        if (cascadeClassifier == null) {
            logger.debug("Logo classifier failed to initialize");
        }
    }

    @Override
    public void process(AssetBuilder asset) {

        if (!asset.isSuperType("image")) {
            return;
        }

        if (asset.contains("Logos")) {
            logger.debug("{} has already been processed by LogoIngestor.", asset);
            return;
        }

        // Perform analysis on the full resolution source image. The VISA and other logos tend to be rather small in
        // the frame, and using the proxy misses many logos.

        Mat image = OpenCVUtils.convert(asset.getImage());
        logger.debug("Starting logo detection on {} mat {}", asset.getFilename(), image);
        // The OpenCV levelWeights thing doesn't seem to work. We'll do a few calls to the detector with different thresholds
        // in order to estimate a confidence value.
        // In the code below, detectMultiScale is called four times, each with a bigger threshold for the number of detections required
        // (and also with a different scaling factor--these values were found by trial and error)
        // Confidence values are assigned depending on when the classifier finds the logo. We use a maximum value of 0.5,
        // in order to save the rest of the range (up to 1.0) to increase the confidence according to logo size.
        // OPTIMIZE: Use the size of the resulting rectangles to tweak minLogo and maxLogo in order to save the detector a bunch of work
        double confidence = 0;
        RectVector logoDetections = new RectVector();
        RectVector newLogoDetections = new RectVector();

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

        long logoCount = 0;
        Size minLogo = new Size(100, 50);
        Size maxLogo = new Size(4000, 2000);

        try {

            for (int i = 0; i < options.size(); i++) {
                cascadeClassifier.detectMultiScale(image, newLogoDetections,
                        options.get(i).scaleFactor, (int) options.get(i).detectionThreshold,
                        0 /*flags*/, minLogo, maxLogo);

                long count = newLogoDetections.size();
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

            final int w = image.size().width();
            final int h = image.size().height();

            for (int i = 0; i < logoDetections.size(); ++i) {
                Rect rect = logoDetections.get(i);
                // Detect points in the area found by the Haar cascade
                int xmin = rect.x();
                if (xmin < 0) xmin = 0;

                int xmax = rect.x() + rect.width();
                if (xmax >= w) xmax = w - 1;

                int ymin = rect.y();
                if (ymin < 0) ymin = 0;

                int ymax = rect.y() + rect.height();
                if (ymax >= h) ymax = h - 1;

                // Logo is big if more than 5% of total height
                double relSize = rect.height() / (double) h;
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
        } finally {
            try {
                image.close();
            } catch (Exception e) {
                logger.warn("Failed to close OpenCV Mat {}", image, e);
            }
        }
    }
}

