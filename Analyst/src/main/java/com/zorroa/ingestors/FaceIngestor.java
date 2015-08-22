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
 * Uses openCV to detect faces
 *
 * @author jbuhler
 *
 */
public class FaceIngestor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FaceIngestor.class);

    private static String cascadeName = "haarcascade_frontalface_alt.xml";


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
                logger.error("FaceIngestor requires ZORROA_OPENCV_MODEL_PATH");
                return null;
            }
            String haarPath = modelPath + "/face/" + cascadeName;
            logger.info("Face processor haarPath path: " + haarPath);
            CascadeClassifier classifier = null;
            try {
                classifier = new CascadeClassifier(haarPath);
                if (classifier != null) {
                    logger.info("Face classifier initialized");
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
    public void process(AssetBuilder asset) {
        String argCascadeName = (String) getArgs().get("CascadeName");
        if (argCascadeName != null) {
            cascadeName = argCascadeName;
        }

        if (!ingestProcessorService.isImage(asset)) {
            return;     // Only process images
        }

        List<Proxy> proxyList = (List<Proxy>) asset.document.get("proxies");
        if (proxyList == null) {
            logger.error("Cannot find proxy list for " + asset.getFilename() + ", skipping Face analysis");
            return;
        }
        String classifyPath = asset.getFile().getPath();
        Size minFace = new Size(15, 15);
        Size maxFace = new Size(200, 200);
        for (Proxy proxy : proxyList) {
            if (proxy.getWidth() >= 500 || proxy.getHeight() >= 500) {
                String proxyName = proxy.getFile();
                proxyName = proxyName.substring(0, proxyName.lastIndexOf('.'));
                classifyPath = ingestProcessorService.getProxyFile(proxyName, "png").getPath();
                minFace.width = minFace.height = proxy.getHeight() / 25;
                maxFace.width = maxFace.height = minFace.width * 20;
                logger.info("Face: minFace = " + minFace.width);
                logger.info("Face: maxFace = " + maxFace.width);

                break;
            }
        }

        // Perform facial analysis
        logger.info("Starting facial detection on " + asset.getFilename());
        Mat image = Highgui.imread(classifyPath);
        MatOfRect faceDetections = new MatOfRect();
        cascadeClassifier.get().detectMultiScale(image, faceDetections, 1.05, 12, 0, minFace, maxFace);
        int faceCount = faceDetections.toArray().length;
        logger.info("Detected " + faceCount + " faces in " + asset.getFilename());
        if (faceCount > 0) {
            String keywords = "face,face" + faceCount;
            logger.info("FaceIngestor: " + keywords);
            List<String> keywordList = Arrays.asList(keywords.split(","));
            asset.map("face", "keywords", "type", "string");
            asset.map("face", "keywords", "copy_to", null);
            asset.put("face", "keywords", keywordList);
        }
    }
}
