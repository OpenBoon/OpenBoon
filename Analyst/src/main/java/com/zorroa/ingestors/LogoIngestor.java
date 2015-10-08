package com.zorroa.ingestors;

import com.zorroa.archivist.sdk.AssetBuilder;
import com.zorroa.archivist.sdk.IngestProcessor;
import com.zorroa.archivist.sdk.Proxy;
import org.opencv.core.*;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.highgui.Highgui;
import org.opencv.objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;
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


    static {
        System.loadLibrary("opencv_java2411");
    }

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

        if (!ingestProcessorService.isImage(asset)) {
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

        MatOfRect logoDetections = new MatOfRect();
        cascadeClassifier.get().detectMultiScale(image, logoDetections, 1.005, 15, 0, minLogo, maxLogo);
        int logoCount = logoDetections.toArray().length;
        logger.info("Detected " + logoCount + " logos in " + asset.getFilename());


        /***********
         * Uncomment to do detection without SIFT
         *
        if (logoCount > 0) {
            logger.info("LogoIngestor: Haar detected " + logoCount + " potential logos.");
            String value = "visa";

            for (Rect rect : logoDetections.toArray()) {
                // Logo is big if more than 5% of total height
                if (rect.height / imSize.height > .05) {
                    value = value + ",bigvisa";
                }

            }
            logger.info("LogoIngestor: " + value);
            String[] keywords = (String[]) Arrays.asList(value.split(",")).toArray();
            asset.putKeywords("Logos", "keywords", keywords);
        }
    }
}
         *****/

        Mat feature = Highgui.imread(featurePath);

        MatOfKeyPoint kp1 = new MatOfKeyPoint();
        MatOfKeyPoint kp2  = new MatOfKeyPoint();

        detector.detect(feature, kp1);

        DescriptorExtractor extractor = DescriptorExtractor.create(2);

        Mat desc1 = new Mat();
        Mat desc2 = new Mat();
        extractor.compute(feature, kp1, desc1);

        //MatOfDMatch matches = new MatOfDMatch();


        if (logoCount > 0) {
            logger.info("LogoIngestor: Haar detected " + logoCount + " potential logos.");

            String value = "visa";
            String svgVal = "<svg>";

            for (Rect rect : logoDetections.toArray()) {

                // Detect points in the area found by the Haar cascade
                int xmin = rect.x;
                if (xmin < 0) xmin = 0;

                int xmax = rect.x+rect.width;
                if (xmax >= imSize.width) xmax = (int)imSize.width-1;

                int ymin = rect.y;
                if (ymin < 0) ymin = 0;

                int ymax = rect.y+rect.height;
                if (ymax >= imSize.height) ymax = (int)imSize.height-1;

                cropImg = image.submat(ymin, ymax, xmin, xmax);
                detector.detect(cropImg, kp2);
                extractor.compute(cropImg, kp2, desc2);


                // I use matcher.knnmatch in the Python version. I don't know how to do this here.
                // I'm confused by the list of matches that I need to give knnmatch as a parameter.
                // The Java code diverges from here on from the Python code...
                // HELP!
                //matcher.match(desc1, desc2, matches);

                //List<List<DMatch>> Lmatches;
                LinkedList<MatOfDMatch> raw_matches=new LinkedList<MatOfDMatch>();
                matcher.knnMatch(desc1, desc2, raw_matches, 2);
                //List<DMatch> matchesList = matches.toList();

                DMatch bestMatch, secondBestMatch;
                double ratio = 0.75;
                int matchCount = 0;


                for (MatOfDMatch matOfDMatch : raw_matches) {
                    bestMatch=matOfDMatch.toArray()[0];
                    secondBestMatch=matOfDMatch.toArray()[1];
                    if (bestMatch.distance / secondBestMatch.distance <= ratio) {
                        matchCount += 1;

                    }
                }

                if (matchCount > 4) {
                    value = "visa2";
                    // Logo is big if more than 5% of total height
                    if (rect.height / imSize.height > .05) {
                        value = value + ",bigvisa";
                    }

                    // Draw rectangle
                    svgVal = svgVal + "<polygon points=\"" + xmin + "," + ymin + " " + xmax + "," + ymin + " " + xmax + "," + ymax + " " + xmin + "," + ymax + "\" style=\"fill:none;stroke:green;stroke-width:2\" />";


                }

            }
            logger.info("LogoIngestor: " + value);
            String[] keywords = (String[]) Arrays.asList(value.split(",")).toArray();
            asset.putKeywords("Logos", "keywords", keywords);

            if (svgVal != "<svg>") {
                svgVal = svgVal + "</svg>";
                asset.putKeyword("SVG", "Logos", svgVal);
            }
        }
    }
}

