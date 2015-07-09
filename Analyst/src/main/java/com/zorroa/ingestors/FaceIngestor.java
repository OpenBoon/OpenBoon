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

    CascadeClassifier faceDetector;


    public FaceIngestor() {
    }

    @Override
    public void process(AssetBuilder asset) {
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
        if (asset.isImageType()) {
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
            System.out.println(String.format("Detected %s faces", faceDetections.toArray().length));
            for (Rect rect : faceDetections.toArray()) {
                Core.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0));
            }
            //Highgui.imwrite(classifyPath, image);
            String keywords = "face";
            logger.info("FaceIngestor " + keywords);
            List<String> keywordList = Arrays.asList(keywords.split(","));
            asset.put("caffe", "keywords", keywordList);
        }
    }
}
