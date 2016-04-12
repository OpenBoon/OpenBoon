package com.zorroa.analyst.ingestors;

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
 * Outputs:         Passes a Map of dominant colors with associated % of pixels
 *
 */

// There will be overlap with ColorHistogramsIngestor - need to address that somehow (ie merge them or find ways
// to re-structure classes to take advantage of common functionality


import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.Proxy;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.TermCriteria;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ColorAnalysisIngestor extends IngestProcessor {

// Smooth + downscale: smooth out small details + artifacts etc + increase speed

    //! Color space used
    //! Default: HSV
    String colorSpace = "Lab";

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
    Map<int[], Float> performVQAnalysis(Mat imgToAnalyze){

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
        for (int i = 0; i < nClusters; i++) {
            logger.info("Cluster: {}, {}, {} ", centers.get(i, 0)[0], centers.get(i, 1)[0], centers.get(i, 2)[0]);
        }

        // Merge clusters whose centers are very close to one another:
        // 1. Compute distances between cluster centers -
        // 2. if less than heuristic, replace centers by average of the two and reassign labels
        // 3. stop when no distances pass the heuristic
        // For now simple coding for rapid dvp / base testing purposes - optimize later
        float mergeHeuristic = 5;
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
        for (int i = 0; i < nClusters; i++) {
            logger.info("Cluster: {}, {}, {}", centers.get(i, 0)[0], centers.get(i, 1)[0], centers.get(i, 2)[0]);
        }

        HashMap<int[], Float> analysisResult = new HashMap<int[], Float>();
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
            int[] center = new int[3];
            center[0] = (int) centers.get(i,0)[0];
            center[1] = (int) centers.get(i,1)[0];
            center[2] = (int) centers.get(i,2)[0];
            if (center[0] != -1 && center[1] != -1 && center[2] != -1) {
                analysisResult.put(center, new Float(tempHolder[i]));
                logger.info("Center: {}, {}, {} - coverage: {}", center[0], center[1], center[2], tempHolder[i] * 100);
            }
        }

        return analysisResult;
    }

    /**
     * Performs uniform quantization assuming a 3-band color space and returns a 3D histogram.
     *
     * @param  ranges value range for each band (ie should be equal to (max possible value - min possible value)
     * @param  nBins  number of bins desired along each dimension
     * @return 3D histogram stored as a map between non-zero histogram bins and count (normalized to be between 0 & 1)
     */
    Map<int[], Float> performUniformQuantizationAnalysis(Mat imgToAnalyze, int[] ranges, int[] nBins){

        float[][][] threedHist = new float[nBins[0]][nBins[1]][nBins[2]];
        for (int i = 0; i < nBins[0]; i++) {
            for (int j = 0; j < nBins[1]; j++) {
                for (int k = 0; k < nBins[2]; k++) {
                    threedHist[i][j][k] = 0;
                }
            }
        }

        // Perform binning - for now hardcoding values to test
        // Need to decide where this will be best residing (this class or Histograms class)
        for (int i = 0; i < imgToAnalyze.rows(); i++) {
            for (int j = 0; j < imgToAnalyze.cols(); j++) {
                double[] data = imgToAnalyze.get(i,j);
                int b1Index = (int) data[0]/(ranges[0] / nBins[0]);
                int b2Index = (int) data[1]/(ranges[1] / nBins[1]);
                int b3Index = (int) data[2]/(ranges[2] / nBins[2]);
                threedHist[b1Index][b2Index][b3Index] += 1.0;
            }
        }

        int numPixels = imgToAnalyze.rows() * imgToAnalyze.cols();
        HashMap<int[], Float> colorAnalysisResults = new HashMap<int[], Float>();
        // Package results
        for (int b1 = 0; b1 < nBins[0]; b1 += 1) {
            for (int b2 = 0; b2 < nBins[1]; b2 += 1) {
                for (int b3 = 0; b3 < nBins[2]; b3 += 1) {
                    if (threedHist[b1][b2][b3] > 0) {
                        int[] bin = {b1, b2, b3};
                        colorAnalysisResults.put(bin, threedHist[b1][b2][b3] / numPixels);
                    }
                }
            }
        }

        return colorAnalysisResults;
    }


    /** Loads the color palette from supplied xml file.
     * Will convert the palette to the color space specified for analysis
     * @param colorPaletteFile The file to read from
     */
    // XXX: This will need to be moved elsewhere so the palette is loaded before the analysis
    // XXX: and passed with the builder
    // XXX: Also right now assuming palette is in RGB.
    Map<int[], String> loadColorPaletteFromXmlFile(File colorPaletteFile) {

        HashMap<int[], String> colorPalette = new HashMap<int[], String>();

        // Basic loading for now.
        // TBD: expand in case the xml file stores several possible palettes.
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            // Load the input XML document, parse it and return an instance of the
            // Document class.
            Document document = builder.parse(colorPaletteFile);
            NodeList nodeList = document.getElementsByTagName("color");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                Element elem = (Element) node;
                String name = elem.getElementsByTagName("name").item(0).getChildNodes().item(0).getNodeValue();
                int[] colorValues = new int[3];
                colorValues[2] = Integer.parseInt(elem.getElementsByTagName("rvalue").item(0).getChildNodes().item(0).getNodeValue());
                colorValues[1] = Integer.parseInt(elem.getElementsByTagName("gvalue").item(0).getChildNodes().item(0).getNodeValue());
                colorValues[0] = Integer.parseInt(elem.getElementsByTagName("bvalue").item(0).getChildNodes().item(0).getNodeValue());
                if (colorSpace.compareTo("Lab") == 0) {
                    Mat rgbColors = new Mat(1, 1, CvType.CV_32FC3);
                    Mat labColors = new Mat(1, 1, CvType.CV_32FC3);
                    float[] fpaletteColor = {(float) (colorValues[0]/255.0f), (float) (colorValues[1]/255.0f), (float) (colorValues[2]/255.0f)};
                    rgbColors.put(0, 0, fpaletteColor);
                    Imgproc.cvtColor(rgbColors, labColors, Imgproc.COLOR_BGR2Lab);
                    colorValues[0] = (int) labColors.get(0,0)[0] * 255 / 100;
                    colorValues[1] = (int) labColors.get(0,0)[1] + 128;
                    colorValues[2] = (int) labColors.get(0,0)[2] + 128;
                    //logger.info("Lab palette color {}: Lab: {}, {}, {}", name, colorValues[0], colorValues[1], colorValues[2]);
                }
                colorPalette.put(colorValues, name);
            }
        }
        catch(IOException | ParserConfigurationException | SAXException e){
            logger.warn("error loading color palette file: {}", colorPaletteFile, e);
            return null;
        }
        // TBD: Catch exceptions
        return colorPalette;

    }

    /**
     * Maps color cluster centers (coordinates in chosen color space) to color names based on a supplied color palette.
     * This is currently performed by finding the nearest palette color (based on pre-defined distance at this time).
     * @param clusters The set of colors to assign names to
     * @param colorPalette The color palette that will be used to map color coordinates to color name
     */
    Map<int[], String> performColorMapping(Map<int[], Float> clusters, Map<int[], String> colorPalette) {

        HashMap<int[], String> colorMappingResults = new HashMap<int[], String>();
        Set<int[]> paletteColors = colorPalette.keySet();
        for (int[] cluster : clusters.keySet()) {
            double minDist = Double.MAX_VALUE;

            String colorName = "";

            for (int[] paletteColor : paletteColors) {

                double d1 = (double)( cluster[0] - paletteColor[0] );
                double d2 = (double)( cluster[1] - paletteColor[1] );
                double d3 = (double)( cluster[2] - paletteColor[2] );

                //double d = Math.sqrt( d1*d1 + d2*d2 + d3*d3);
                double d = Math.abs(d1) + Math.abs(d2) + Math.abs(d3);
                if ( d < minDist ) {
                    minDist = d;
                    colorName = colorPalette.get(paletteColor);
                }
            }
            colorMappingResults.put(cluster, colorName);
            float coverage = ((Float) clusters.get(cluster)).floatValue() * 100;
            logger.info("Dominant color: {}, {}, {} -> {} w. coverage: {}", cluster[0], cluster[1], cluster[2], colorName, Float.toString(coverage));
        }
        return colorMappingResults;
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
            asset.setAttr("ColorAnalysis", "ColorClusters", null);
            asset.setAttr("ColorAnalysis", "ColorMapping", null);
            return;     // Only process images
        }

        // Open files to perform analysis on. If proxies are available, use the smallest one
        // up to 128x128. If none is available, throw exception (as per convo with Dan).
        String filePath = asset.getFile().getPath();
        logger.info("Starting color analysis for: {} ", filePath);
        Mat imgToAnalyze = null;
        List<Proxy> proxyList = (List<Proxy>) asset.getDocument().get("proxies");
        if (proxyList == null) {
            logger.error("Cannot find proxy list for: '{}'. Skipping Color Analysis.", filePath);
            asset.setAttr("ColorAnalysis", "ColorClusters", null);
            asset.setAttr("ColorAnalysis", "ColorMapping", null);
            return; // TBD: Add exception throw (it is not part of interface is it?)
        }
        else {
            int minW = Integer.MAX_VALUE;
            int minH = Integer.MAX_VALUE;
            Proxy proxyToUse = null;
            for (Proxy proxy : proxyList) {
                int proxyW = proxy.getWidth();
                int proxyH = proxy.getHeight();
                if (proxyW >= 128 || proxyH >= 128 && proxyW < minW && proxyH < minH) {
                    proxyToUse = proxy;
                    minW = proxyW;
                    minH = proxyH;
                }
            }
            if(proxyToUse != null) {
                imgToAnalyze = Highgui.imread(proxyToUse.getPath());
                if (imgToAnalyze.empty()) {
                    logger.warn("Cannot open proxy with path: '{}'. Skipping Color Analysis.", proxyToUse.getPath());
                    asset.setAttr("ColorAnalysis", "ColorClusters", null);
                    asset.setAttr("ColorAnalysis", "ColorMapping", null);
                }
            }
            else {
                logger.warn("Proxy selection for the Color Analysis yielded null result. Skipping Color Analysis.");
                asset.setAttr("ColorAnalysis", "ColorClusters", null);
                asset.setAttr("ColorAnalysis", "ColorMapping", null);
            }
        }


        // Load color palette
        Map<String, String> env = System.getenv();
        String modelPath = env.get("ZORROA_OPENCV_MODEL_PATH"); // XXX TBD: Change that to ZORROA_MODEL_PATH
        if (modelPath == null) {
            logger.error("ColorAnalysisIngestor requires ZORROA_OPENCV_MODEL_PATH");
            asset.setAttr("ColorAnalysis", "ColorClusters", null);
            asset.setAttr("ColorAnalysis", "ColorMapping", null);
            return;
        }
        String resourcePath = modelPath + "/color/";
        File colorPaletteFile = new File(resourcePath + "ColorPalettes.xml"); // XXX TBD: Don't hard code filename here
        Map<int[], String> colorPalette = loadColorPaletteFromXmlFile(colorPaletteFile);
        if (colorPalette == null) {
            logger.error("Color Palette could not be loaded.");
        }

        // Read parameters
        if (getArgs().get("ColorSpace") != null) {
            String argColorSpace = (String) getArgs().get("ColorSpace");
            if (argColorSpace.compareTo("HSV")!= 0 && argColorSpace.compareTo("Lab") != 0 && argColorSpace.compareTo("BGR") != 0) {
                logger.info("{} is not a recognized color space. Switching to default BGR", argColorSpace);
            }
            else {
                colorSpace = argColorSpace;
            }
        }
        else {
            logger.info("No color space specified, using default color space: {}", colorSpace);
        }

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

        /*
        int[] ranges = {256, 256, 256};
        int[] nBins = {4, 4, 4};
        HashMap<int[], Float> uniQAnalysisResults = performUniformQuantizationAnalysis(imgToAnalyze, ranges, nBins);
        logger.info("==== Uniform quantization analysis results: ====");
        Set<Map.Entry<int[],Float>> resultsSet = uniQAnalysisResults.entrySet();
        for (Iterator it = resultsSet.iterator(); it.hasNext();) {
            Map.Entry<int[],Float> entry = (Map.Entry<int[],Float>)it.next();
            int[] bin = entry.getKey();
            Float value = entry.getValue();
            float percentage = value.floatValue() * 100;
            logger.info("Bin: " + bin[0] + "," + bin[1] + "," + bin[2] + " - " + Float.toString(percentage));
        }
        */

        logger.info("=== Color Analysis results ===");
        Map<int[], Float> colorAnalysisResults = performVQAnalysis(imgToAnalyze);
        Map<int[], String> colorMappingResults = performColorMapping(colorAnalysisResults, colorPalette);

        // Finalize outputs passed via assets
        asset.setAttr("ColorAnalysis", "ColorClusters", colorAnalysisResults);
        asset.setAttr("ColorAnalysis", "ColorMapping", colorMappingResults);
    }

}
