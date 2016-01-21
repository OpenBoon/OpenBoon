package com.zorroa.ingestors;

/**
 * Created by barbara on 1/9/16.
 *
 * This Ingestor performs the following:
 * 1. Identifies the color palette that will be used for analysis
 * 2. For each pixel, finds the color it is associated with and adds that pixel to the color count
 * 3. Outputs the N colors with the biggest counts in descending order - if a color table is available, color
 * The color palette can be:
 *    a. pre-computed (in which case it is loaded from a file ?);
 *    b. can be based on uniform of the color space
 *    c. can be based on clustering/non-uniform quantization techniques using the image itself or a set of images
 *    (at this time using K-means to leverage openCV fairly rapidly - other methods possible and to follow potentially)
 * This Ingestor leverages OpenCV and pure Java.
 *
 * Expected parameters:
 * "ColorSpace" :   Can be one of "BGR" (default), "HSV", "Lab" (Cie Lab) - more spaces may be supported in the future
 *                  as need be
 * "PaletteType" :  One of: "Pre-computed", "Fixed", "Quantization"
 *
 * Outputs:         Passes a list of Float type values for each histogram (one for each band) computed to the asset
 *                  Note: at this time, histograms are not normalized (which can be left to the downstream operators
 *                  depending on what needs to be performed next; this may be changed though.
 *
 */

// There will be overlap with ColorHistogramsIngestor - need to address that somehow (ie merge them or find ways
// to re-structure classes to take advantage of common functionality

import java.util.Iterator;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;


public class ColorAnalysisIngestor extends IngestProcessor {

// Smooth + downscale: smooth out small details + artifacts etc + increase speed

    //! Color space used
    //! Default: HSV
    String colorSpace = "BGR";

    //! Logger
    private static final Logger logger = LoggerFactory.getLogger(ColorAnalysisIngestor.class);

    //! OpenCV
    private static OpenCVLoader openCVLoader = new OpenCVLoader();

    /** Default constructor
     */
    public ColorAnalysisIngestor() { }

    /**
     * Identifies dominant colors through Vector Quantization.
     * Currently uses K-means clustering technique to take advantage as it is readily available
     * with OpenCV. However, another VQ technique could be used.
     */
    public HashMap<double[], Float> performVQAnalysis(Mat imgToAnalyze){

        /*
            Identify clusters.
         */

        Mat samples = new Mat(imgToAnalyze.rows() * imgToAnalyze.cols(), 1, CvType.CV_8UC3);
        for (int i = 0; i < imgToAnalyze.rows(); i++) {
            for (int j = 0; j < imgToAnalyze.cols(); j++) {
                samples.put((i * imgToAnalyze.rows() + j), 0, imgToAnalyze.get(i, j));
            }
        }


        Mat labels = new Mat();
        TermCriteria criteria = new TermCriteria(TermCriteria.COUNT, 100, 1);
        Mat samples32f = new Mat();
        samples.convertTo(samples32f, CvType.CV_32F, 1.0 );
        Mat centers = new Mat();
        int nClusters = 8;
        Core.kmeans(samples32f, nClusters, labels, criteria, 1, Core.KMEANS_PP_CENTERS, centers);
        logger.info("Completed k-means clustering.");
        logger.info("==== Initial Clusters =====");
        for (int i = 0; i < 8; i++) {
            logger.info("Cluster: " + centers.get(i, 0)[0] + "," + centers.get(i, 1)[0] + "," + centers.get(i, 2)[0]);
        }

        // Merge clusters whose centers are very close to one another:
        // 1. Compute distances between cluster centers -
        // 2. if less than heuristic, replace centers by average of the two and reassign labels
        // 3. stop when no distances pass the heuristic
        // For now simple coding for rapid dvp / base testing purposes - optimize later
        float mergeHeuristic = 30;
        while (true ) {
            boolean foundClustersToMerge = false;
            for (int i = 0; i < nClusters; i++) {
                for (int j = 0; j < nClusters; j++) {
                    if ( i != j && (centers.get(i,0)[0] != -1 && centers.get(i,1)[0] != -1 && centers.get(i,2)[0] != -1)
                                && (centers.get(j,0)[0] != -1 && centers.get(j,1)[0] != -1 && centers.get(j,2)[0] != -1) ){
                        Mat c1 = new Mat(1, 3, CvType.CV_32F);
                        Mat c2 = new Mat(1, 3, CvType.CV_32F);
                        c1.put(0, 0, centers.get(i,0)[0]);
                        c1.put(0, 1, centers.get(i,1)[0]);
                        c1.put(0, 2, centers.get(i,2)[0]);
                        c2.put(0, 0, centers.get(j,0)[0]);
                        c2.put(0, 1, centers.get(j,1)[0]);
                        c2.put(0, 2, centers.get(j,2)[0]);
                        double d = Core.norm(c1, c2, Core.NORM_L2);
                        if (d < mergeHeuristic) {
                            foundClustersToMerge = true;
                            // Replace first center by new cluster center
                            // Mark 2nd center as no longer valid
                            centers.put(i, 0, (centers.get(i,0)[0] + centers.get(i,0)[0]) / 2.0);
                            centers.put(i, 1, (centers.get(i,1)[0] + centers.get(i,1)[0]) / 2.0);
                            centers.put(i, 2, (centers.get(i,2)[0] + centers.get(i,2)[0]) / 2.0);
                            centers.put(j, 0, -1); // This will work for color spaces we typicall use
                            centers.put(j, 1, -1);
                            centers.put(j, 2, -1);
                            for (int k = 0; k < labels.rows(); k++) {
                                if (labels.get(k,0)[0] == j) {
                                    labels.put(k,0,i);
                                }
                            }
                        }
                    }
                }

            }
            if (foundClustersToMerge == false) {
                break;
            }
        }

        logger.info("==== New Clusters =====");
        for (int i = 0; i < 8; i++) {
            logger.info("Cluster: " + centers.get(i, 0)[0] + "," + centers.get(i, 1)[0] + "," + centers.get(i, 2)[0]);
        }

        HashMap<double[], Float> analysisResult = new HashMap<double[], Float>();
        float[] tempHolder = new float[centers.rows()];
        // TBD: At some point, replace with different structure than mat further up because there
        // does not seem to be efficient mechanism to iterate Mat on Java side
        for (int i = 0; i < centers.rows(); i++) {
            tempHolder[i] = 0;
        }
        for (int i = 0; i < labels.rows(); i++) {
            int index = (int) labels.get(i,0)[0];
            tempHolder[ index ] += 1.0 / labels.rows();
        }

        logger.info("==== Final VQ Analysis Results =====");
        for (int i = 0; i < centers.rows(); i++) {
            double[] center = new double[3];
            center[0] = centers.get(i,0)[0];
            center[1] = centers.get(i,1)[0];
            center[2] = centers.get(i,2)[0];
            if (center[0] != -1 && center[1] != -1 && center[2] != -1) {
                analysisResult.put(center, new Float(tempHolder[i]));
                logger.info("Center: " + center[0] + "," + center[1] + "," + center[2] + " - %age: " + tempHolder[i] * 100);
            }
        }

        return analysisResult;
    }

    /**
     * Performs uniform quantization.
     */
    public HashMap<double[], Float> performUniformQuantizationAnalysis(Mat imgToAnalyze){

        float[][][] threedHist = new float[4][4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    threedHist[i][j][k] = 0;
                }
            }
        }

        // Perform binning - for now hardcoding values to test
        // Need to decide where this will be best residing (this class or Histograms class)
        int numElements = imgToAnalyze.rows() * imgToAnalyze.cols();
        float histIncrement = 1.0f / (float) numElements;
        for (int i = 0; i < imgToAnalyze.rows(); i++) {
            for (int j = 0; j < imgToAnalyze.cols(); j++) {
                double[] data = imgToAnalyze.get(i,j);
                int b1Index = (int) data[0]/(256 / 4);
                int b2Index = (int) data[1]/(256 / 4);
                int b3Index = (int) data[2]/(256 / 4);
                threedHist[b1Index][b2Index][b3Index] += histIncrement;
            }
        }

        HashMap<double[], Float> colorAnalysisResults = new HashMap<double[], Float>();
        // Package results
        for (int b1 = 0; b1 < 256 / 4; b1 += 1) {
            for (int b2 = 0; b2 < 256 / 4; b2 += 1) {
                for (int b3 = 0; b3 < 256 / 4; b3 += 1) {
                    if (threedHist[b1][b2][b3] > 0) {
                        double[] bin = {b1, b2, b3};
                        colorAnalysisResults.put(bin, threedHist[b1][b2][b3]);
                    }
                }
            }
        }

        return colorAnalysisResults;
    }

    /** Performs the color analysis on the supplied asset
     @param asset The asset to analyse
     */
    @Override
    public void process(AssetBuilder asset) {

        // Check that the asset is indeed an image and load the image file
        // By default, we load the image as a BGR image

        if (!asset.isSuperType("image")) {
            logger.info("Asset is not an image -> Color Analysis not performed.");
            return;     // Only process images
        }

        // Read parameters
        if (getArgs().get("ColorSpace") != null) {
            String argColorSpace = (String) getArgs().get("ColorSpace");
            if (argColorSpace.compareTo("HSV")!= 0 && argColorSpace.compareTo("Lab") != 0 && argColorSpace.compareTo("BGR") != 0) {
                logger.info(argColorSpace + " is not a recognized color space. Switching to default BGR");
            }
            else {
                colorSpace = argColorSpace;
            }
        }
        else {
            logger.info("No color space specified, using default color space: " + colorSpace);
        }

        String filePath = asset.getFile().getPath();
        logger.info("Opening image file: " + asset.getFilename());
        Mat imgToAnalyze = Highgui.imread(filePath, Highgui.CV_LOAD_IMAGE_COLOR);
        if (imgToAnalyze.empty()) {
            logger.info("Could not read image file: " + filePath);
            return;
        }

        /*
         If required blur and downsample image to a lower resolution for analysis.
         Proxy could also be used but need more info.
         */
        /*
        Mat[] pyrLevs = new Mat[4];
        pyrLevs[0] = imgToAnalyze;
        for (int i = 1; i < 4; i++) {
            pyrLevs[i] = new Mat();
            Imgproc.pyrDown(pyrLevs[i-1], pyrLevs[i]);
            String filename = "Level"+i+".jpg";
            Highgui.imwrite(filename, pyrLevs[i]);

        }
        */

        /*
         If required, convert image to the desired color space
         Use OpenCV
          */

        if (colorSpace.compareTo("Lab") == 0) {
            Imgproc.cvtColor(imgToAnalyze, imgToAnalyze, Imgproc.COLOR_BGR2Lab);
        }
        else if (colorSpace.compareTo("HSV") == 0) {
            Imgproc.cvtColor(imgToAnalyze, imgToAnalyze, Imgproc.COLOR_BGR2HSV);
        }

        HashMap<double[], Float> colorAnalysisResults = performVQAnalysis(imgToAnalyze);
        asset.setAttr("ColorAnalysis", "ColorClusters", colorAnalysisResults);
        // Final output includes color mapping - will be completed week of 01/18/2016
    }

}
