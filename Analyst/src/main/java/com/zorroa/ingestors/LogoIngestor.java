package com.zorroa.ingestors;

import com.zorroa.archivist.sdk.AssetBuilder;
import com.zorroa.archivist.sdk.IngestProcessor;
import com.zorroa.archivist.sdk.Proxy;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
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


    static {
        System.loadLibrary("opencv_java2411");
    }

    // CascadeClassifier is not thread-safe, so give one to each thread
    private static final ThreadLocal<CascadeClassifier> cascadeClassifier = new ThreadLocal<CascadeClassifier>(){
        @Override
        protected CascadeClassifier initialValue() {
            Map<String, String> env = System.getenv();
            String modelPath = env.get("ZORROA_OPENCV_MODEL_PATH");
            if (modelPath == null) {
                logger.error("LogoIngestor requires ZORROA_OPENCV_MODEL_PATH");
                return null;
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

    public LogoIngestor() { }

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
        Size minLogo = new Size(20, 10);
        Size maxLogo = new Size(1000, 500);
        for (Proxy proxy : proxyList) {
            if (proxy.getWidth() >= 1024 || proxy.getHeight() >= 1024) {
                String proxyName = proxy.getFile();
                proxyName = proxyName.substring(0, proxyName.lastIndexOf('.'));
                classifyPath = ingestProcessorService.getProxyFile(proxyName, "png").getPath();
                break;
            }
        }

        // Perform analysis
        logger.info("Starting logo detection on " + asset.getFilename());
        Mat image = Highgui.imread(classifyPath);
        MatOfRect logoDetections = new MatOfRect();
        cascadeClassifier.get().detectMultiScale(image, logoDetections, 1.001, 15, 0, minLogo, maxLogo);
        int logoCount = logoDetections.toArray().length;
        logger.info("Detected " + logoCount + " logos in " + asset.getFilename());
        if (logoCount > 0) {
            String value = "visa";
            logger.info("LogoIngestor: " + value);
            String[] keywords = (String[]) Arrays.asList(value.split(",")).toArray();
            asset.putKeywords("Logos", "keywords", keywords);
        }
    }
}
