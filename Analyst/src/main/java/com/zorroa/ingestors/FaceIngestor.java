package com.zorroa.ingestors;

import java.util.List;
import java.util.Arrays;
import java.util.Map;

import com.zorroa.archivist.sdk.IngestProcessor;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zorroa.archivist.sdk.AssetBuilder;
import com.zorroa.archivist.sdk.Proxy;



/**
 *
 * Uses openCV to detect faces
 *
 * @author jbuhler
 *
 */
public class FaceIngestor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FaceIngestor.class);

    static{ System.loadLibrary("opencv_java2411"); }

    private static CascadeClassifier faceDetector;


    public FaceIngestor() {
        if (faceDetector == null) {
            Map<String, String> env = System.getenv();
            String modelPath = env.get("ZORROA_OPENCV_MODEL_PATH");
            if (modelPath == null) {
                logger.error("CaffeIngestor requires ZORROA_OPENCV_MODEL_PATH");
                return;
            }
            String haarPath = modelPath + "/face/haarcascade_frontalface_alt.xml";
            logger.info("Face processor haarPath path: " + haarPath);
            faceDetector = new CascadeClassifier(haarPath);
            logger.info("FaceIngestor initialized");
        }
    }

    @Override
    public void process(AssetBuilder asset) {
        if (ingestProcessorService.isImage(asset)) {
            // Start with the original image, but try to find a proxy to use for classification
            String classifyPath = asset.getFile().getPath();
            List<Proxy> proxyList = (List<Proxy>) asset.document.get("proxies");
            if (proxyList == null) {
                logger.error("Cannot find proxy list");
            } else {
                for (Proxy proxy : proxyList) {
                    if (proxy.getWidth() >= 256 || proxy.getHeight() >= 256) {
                        String proxyName = proxy.getFile();
                        proxyName = proxyName.substring(0, proxyName.lastIndexOf('.'));
                        classifyPath = ingestProcessorService.getProxyFile(proxyName, "png").getPath();
                        break;
                    }
                }
            }
            Mat image = Highgui.imread(classifyPath);
            MatOfRect faceDetections = new MatOfRect();
            faceDetector.detectMultiScale(image, faceDetections);
            int faceCount = faceDetections.toArray().length;
            logger.info("Detected " + faceCount + " faces in " + asset.getFilename());
            if (faceCount > 0) {
                String keywords = "face";
                logger.info("FaceIngestor: " + keywords);
                List<String> keywordList = Arrays.asList(keywords.split(","));
                asset.map("face", "keywords", "type", "string");
                asset.map("face", "keywords", "copy_to", null);
                asset.put("face", "keywords", keywordList);
            }
        }
    }
}
