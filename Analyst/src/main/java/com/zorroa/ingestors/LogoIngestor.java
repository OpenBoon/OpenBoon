package com.zorroa.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.AssetType;
import com.zorroa.archivist.sdk.domain.Proxy;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.opencv.core.*;

import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
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
    private static DescriptorMatcher matcher;
    private static FeatureDetector detector;
    private static String featurePath;
    private static OpenCVLoader openCVLoader = new OpenCVLoader();

    // CascadeClassifier is not thread-safe, so give one to each thread
    private static final ThreadLocal<CascadeClassifier> cascadeClassifier = new ThreadLocal<CascadeClassifier>() {
        @Override
        protected CascadeClassifier initialValue() {
            Map<String, String> env = System.getenv();
            String modelPath = env.get("ZORROA_OPENCV_MODEL_PATH");
            if (modelPath == null) {
                logger.error("LogoIngestor requires ZORROA_OPENCV_MODEL_PATH");
                return null;
            }

            if (matcher == null) {
                featurePath = modelPath + "/feature/visa.jpg";
                logger.info("Feature processor feature path: " + featurePath);
                detector = FeatureDetector.create(FeatureDetector.SIFT);
                matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
            }


            String haarPath = modelPath + "/logo/" + cascadeName;
            logger.info("Logo processor haarPath path: " + haarPath);
            CascadeClassifier classifier = null;
            try {
                classifier = new CascadeClassifier(haarPath);
                if (classifier != null) {
                    logger.info("Logo classifier initialized");
                }
            } catch (Exception e) {
                logger.error("Logo classifier failed to initialize: " + e.toString());
            } finally {
                return classifier;
            }
        }
    };

    public LogoIngestor() {
    }

    @Override
    public void process(AssetBuilder asset) {
        String argCascadeName = (String) getArgs().get("CascadeName");
        if (argCascadeName != null) {
            cascadeName = argCascadeName;
        }

        if (!asset.isType(AssetType.Image)) {
            return;     // Only process images
        }

        List<Proxy> proxyList = (List<Proxy>) asset.getDocument().get("proxies");

        if (proxyList == null) {
            logger.error("Cannot find proxy list for " + asset.getFilename() + ", skipping Logo analysis");
            return;
        }
        String classifyPath = asset.getFile().getPath();
        Size minLogo = new Size(100, 50);
        Size maxLogo = new Size(4000, 2000);

        // Perform analysis
        logger.info("Starting logo detection on " + asset.getFilename());
        Mat image = Highgui.imread(classifyPath);
        Mat cropImg;
        Size imSize = image.size();

        //MatOfInt rejectLevels = new MatOfInt();
        //MatOfDouble levelWeights = new MatOfDouble();
        //boolean outputRejectLevels = false;
        //cascadeClassifier.get().detectMultiScale(image, logoDetections, rejectLevels, levelWeights, 1.005, 5, 0, minLogo, maxLogo, outputRejectLevels);

        // The OpenCV levelWeights thing doesn't seem to work. We'll do a few calls to the detector with different thresholds
        // in order to estimate a confidence value
        // OPTIMIZE: Use the size of the resulting rectangles to tweak minLogo and maxLogo in order to save the detector a bunch of work
        double confidence = 0;
        MatOfRect logoDetections = new MatOfRect();

        cascadeClassifier.get().detectMultiScale(image, logoDetections, 1.0075, 20, 0, minLogo, maxLogo);
        int logoCount = logoDetections.toArray().length;
        if (logoCount > 0) {
            confidence = 0.2;
            cascadeClassifier.get().detectMultiScale(image, logoDetections, 1.0075, 30, 0, minLogo, maxLogo);
            int Count = logoDetections.toArray().length;
            if (Count > 0) {
                confidence = .4;
                logoCount = Count;
                cascadeClassifier.get().detectMultiScale(image, logoDetections, 1.005, 40, 0, minLogo, maxLogo);
                Count = logoDetections.toArray().length;
                if (Count > 0) {
                    confidence = .6;
                    logoCount = Count;
                    cascadeClassifier.get().detectMultiScale(image, logoDetections, 1.005, 60, 0, minLogo, maxLogo);
                    Count = logoDetections.toArray().length;
                    if (Count > 0) {
                        confidence = .8;
                        logoCount = Count;
                    }
                }
            }
        }

        if (logoCount > 0) {
            logger.info("LogoIngestor: Haar detected " + logoCount + " potential logos.");

            String value = "visa";
            String svgVal = "<svg>";

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
                if (rect.height / imSize.height > .05) {
                    confidence += .2;
                    value = value + ",bigvisa";
                }

                // Draw rectangle
                svgVal += "<polygon points=\"" + xmin + "," + ymin + " " + xmax + "," + ymin + " " + xmax + "," + ymax + " " + xmin + "," + ymax + "\" style=\"fill:none;stroke:green;stroke-width:2\" />";

            }

            logger.info("LogoIngestor: " + value);
            value = value + ",visa" + confidence;
            List<String> keywords = Arrays.asList(value.split(","));
            asset.addKeywords(confidence, true, keywords);
            asset.put("Logos", "keywords", (String[]) keywords.toArray());

            if (svgVal != "<svg>") {
                svgVal += "</svg>";
                asset.put("SVG", "Logos", svgVal);
            }
        }
    }
}

