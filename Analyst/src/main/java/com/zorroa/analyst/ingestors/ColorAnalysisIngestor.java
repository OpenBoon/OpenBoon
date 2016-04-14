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


import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Floats;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.Color;
import com.zorroa.archivist.sdk.domain.Proxy;
import com.zorroa.archivist.sdk.exception.IngestException;
import com.zorroa.archivist.sdk.processor.Argument;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.ProxySchema;
import com.zorroa.archivist.sdk.util.Json;
import org.bytedeco.javacpp.indexer.DoubleIndexer;
import org.bytedeco.javacpp.indexer.FloatBufferIndexer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.IntBufferIndexer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacpp.opencv_imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class ColorAnalysisIngestor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ColorAnalysisIngestor.class);

    @Argument
    String colorSpace = "Lab";

    @Argument
    Integer numClusters = 8;

    private Map<int[], Color> colorPalette;

    @Override
    public  void init() {
        try {
            colorPalette = loadColorPalette();
        } catch (IOException e) {
            logger.warn("Failed to load color palette", e);
            throw new IngestException("Failed to load color palette");
        }
    }

    private HashMap<int[], Color> loadColorPalette() throws IOException {
        List<Color> colors = Json.Mapper.readValue(new File("models/color/colors.json"),
                new TypeReference<List<Color>>() {});
        HashMap<int[], Color> colorPalette = Maps.newHashMapWithExpectedSize(colors.size());

        final boolean isLab = colorSpace.equals("Lab");

        for (Color color: colors) {
            int[] colorValues = new int[]{
                    color.getBlue(),
                    color.getGreen(),
                    color.getRed()

            };

            if (isLab) {
                opencv_core.Mat rgbColors = new opencv_core.Mat(1, 1, opencv_core.CV_32FC3);
                opencv_core.Mat labColors = new opencv_core.Mat(1, 1, opencv_core.CV_32FC3);
                FloatBufferIndexer labColorsIndexer = labColors.createIndexer();
                FloatIndexer rgbColorsIndexer = rgbColors.createIndexer();

                float[] fpaletteColor = {(float) (colorValues[0] / 255.0f), (float) (colorValues[1] / 255.0f), (float) (colorValues[2] / 255.0f)};
                rgbColorsIndexer.put(0, 0, fpaletteColor);
                opencv_imgproc.cvtColor(rgbColors, labColors, opencv_imgproc.COLOR_BGR2Lab);
                colorValues[0] = (int) labColorsIndexer.get(0, 0, 0) * 255 / 100;
                colorValues[1] = (int) labColorsIndexer.get(0, 0, 1) + 128;
                colorValues[2] = (int) labColorsIndexer.get(0, 0, 2) + 128;
            }

            colorPalette.put(colorValues, color);
        }

        return colorPalette;
    }

    /**
     * Identifies dominant colors through Vector Quantization.
     * Currently uses K-means clustering technique to take advantage as it is readily available
     * with OpenCV. However, another VQ technique could be used.
     */
    private Map<int[], Float> performVQAnalysis(opencv_core.Mat imgToAnalyze) {

        opencv_core.Mat samples = imgToAnalyze.reshape(1, imgToAnalyze.cols() * imgToAnalyze.rows());
        samples.convertTo(samples, opencv_core.CV_32F);

        opencv_core.Mat labels = new opencv_core.Mat();
        opencv_core.Mat centers = new opencv_core.Mat();

        opencv_core.TermCriteria criteria = new opencv_core.TermCriteria(
                opencv_core.TermCriteria.EPS + opencv_core.TermCriteria.MAX_ITER, 10, 1.0);

        opencv_core.kmeans(samples, numClusters, labels, criteria, 1,
                opencv_core.KMEANS_RANDOM_CENTERS, centers);

        FloatBufferIndexer centersIndexer = centers.createIndexer();
        for (int i = 0; i < numClusters; i++) {
            logger.debug("Cluster: {}, {}, {} ", centersIndexer.get(i, 0), centersIndexer.get(i, 1), centersIndexer.get(i, 2));
        }

        /**
         * Merge clusters whose centers are very close to one another.
         * 1. Compute distances between cluster centers
         * 2. if less than heuristic, replace centers by average of the two and reassign labels
         * 3. stop when no distances pass the heuristic
         * For now simple coding for rapid dvp / base testing purposes - optimize later
         *
         */

        IntBufferIndexer labelsIndexer = labels.createIndexer();

        /**
         * This is currently broken, not sure how to fix it.
         */
        //normalize(labels, labelsIndexer, numClusters, centersIndexer);

        /**
         * TODO: At some point, replace with different structure than mat further up because there
         * does not seem to be efficient mechanism to iterate Mat on Java side
         */
        HashMap<int[], Float> analysisResult = new HashMap<>();
        float[] tempHolder = new float[centers.rows()];
        Arrays.fill(tempHolder, 0);

        for (int i = 0; i < labels.rows(); i++) {
            int index = labelsIndexer.get(i,0);
            tempHolder[index] += 1.0 / labels.rows();
        }

        for (int i = 0; i < centers.rows(); i++) {
            int[] center = new int[3];
            center[0] = (int) centersIndexer.get(i,0);
            center[1] = (int) centersIndexer.get(i,1);
            center[2] = (int) centersIndexer.get(i,2);
            if (center[0] != -1 && center[1] != -1 && center[2] != -1) {
                analysisResult.put(center, new Float(tempHolder[i]));
            }
        }

        return analysisResult;
    }

    /**
     * This method is broken.
     * error: (-215) mask.empty() || mask.type() == CV_8U in function norm
     *
     * @param labels
     * @param labelsIndexer
     * @param nClusters
     * @param centersIndexer
     */
    private void normalize(opencv_core.Mat labels, IntBufferIndexer labelsIndexer, int nClusters, FloatBufferIndexer centersIndexer) {
        float mergeHeuristic = 5;
        while (true ) {
            boolean foundClustersToMerge = false;
            for (int i = 0; i < nClusters; i++) {
                for (int j = 0; j < nClusters; j++) {
                    if ( i != j && (centersIndexer.get(i,0) != -1 && centersIndexer.get(i,1) != -1 && centersIndexer.get(i,2) != -1)
                            && (centersIndexer.get(j,0) != -1 && centersIndexer.get(j,1) != -1 && centersIndexer.get(j,2) != -1) ){
                        opencv_core.Mat c1 = new opencv_core.Mat(1, 3, opencv_core.CV_32F);
                        opencv_core.Mat c2 = new opencv_core.Mat(1, 3, opencv_core.CV_32F);

                        FloatBufferIndexer c1Indexer = c1.createIndexer();
                        FloatBufferIndexer c2Indexer = c2.createIndexer();
                        c1Indexer.put(0, 0, centersIndexer.get(i,0));
                        c1Indexer.put(0, 1, centersIndexer.get(i,1));
                        c1Indexer.put(0, 2, centersIndexer.get(i,2));
                        c2Indexer.put(0, 0, centersIndexer.get(j,0));
                        c2Indexer.put(0, 1, centersIndexer.get(j,1));
                        c2Indexer.put(0, 2, centersIndexer.get(j,2));

                        double d = opencv_core.norm(c1, opencv_core.NORM_L2, c2);
                        if (d < mergeHeuristic) {
                            foundClustersToMerge = true;
                            // Replace first center by new cluster center
                            // Mark 2nd center as no longer valid
                            centersIndexer.put(i, 0, (centersIndexer.get(i,0) + centersIndexer.get(i,0)) / 2.0f);
                            centersIndexer.put(i, 1, (centersIndexer.get(i,1) + centersIndexer.get(i,1)) / 2.0f);
                            centersIndexer.put(i, 2, (centersIndexer.get(i,2) + centersIndexer.get(i,2)) / 2.0f);
                            centersIndexer.put(j, 0, -1); // This will work for color spaces we typicall use
                            centersIndexer.put(j, 1, -1);
                            centersIndexer.put(j, 2, -1);
                            for (int k = 0; k < labels.rows(); k++) {
                                if (labelsIndexer.get(k,0) == j) {
                                    labelsIndexer.put(k,0,i);
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
            logger.info("Cluster: {}, {}, {}", centersIndexer.get(i, 0), centersIndexer.get(i, 1), centersIndexer.get(i, 2));
        }
    }

    /**
     * Performs uniform quantization assuming a 3-band color space and returns a 3D histogram.
     *
     * @param  ranges value range for each band (ie should be equal to (max possible value - min possible value)
     * @param  nBins  number of bins desired along each dimension
     * @return 3D histogram stored as a map between non-zero histogram bins and count (normalized to be between 0 & 1)
     */
    private Map<int[], Float> performUniformQuantizationAnalysis(opencv_core.Mat imgToAnalyze, int[] ranges, int[] nBins){

        float[][][] threedHist = new float[nBins[0]][nBins[1]][nBins[2]];
        for (int i = 0; i < nBins[0]; i++) {
            for (int j = 0; j < nBins[1]; j++) {
                for (int k = 0; k < nBins[2]; k++) {
                    threedHist[i][j][k] = 0;
                }
            }
        }

        DoubleIndexer imgToAnalyzeIndexer = imgToAnalyze.createIndexer();

        // Perform binning - for now hardcoding values to test
        // Need to decide where this will be best residing (this class or Histograms class)
        for (int i = 0; i < imgToAnalyze.rows(); i++) {
            for (int j = 0; j < imgToAnalyze.cols(); j++) {
                double[] data = new double[3];
                imgToAnalyzeIndexer.get(i, j, data);

                int b1Index = (int) data[0]/(ranges[0] / nBins[0]);
                int b2Index = (int) data[1]/(ranges[1] / nBins[1]);
                int b3Index = (int) data[2]/(ranges[2] / nBins[2]);
                threedHist[b1Index][b2Index][b3Index] += 1.0;
            }
        }

        int numPixels = imgToAnalyze.rows() * imgToAnalyze.cols();
        HashMap<int[], Float> colorAnalysisResults = new HashMap<>();
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

    /**
     * Maps color cluster centers (coordinates in chosen color space) to color names based on a supplied color palette.
     * This is currently performed by finding the nearest palette color (based on pre-defined distance at this time).
     * @param clusters The set of colors to assign names to
     */
    private List<Color> getMappedColors(Map<int[], Float> clusters) {
        Map<String, Color> result = Maps.newHashMapWithExpectedSize(numClusters);
        Set<int[]> paletteColors = colorPalette.keySet();

        for (Map.Entry<int[], Float> entry : clusters.entrySet()) {
            int[] cluster = entry.getKey();
            float coverage = entry.getValue() * 100;
            double minDist = Double.MAX_VALUE;
            Color color = null;
            for (int[] paletteColor : paletteColors) {
                double d1 = (double)( cluster[0] - paletteColor[0] );
                double d2 = (double)( cluster[1] - paletteColor[1] );
                double d3 = (double)( cluster[2] - paletteColor[2] );
                double d = Math.sqrt((d1*d1) + (d2*d2) + (d3*d3));
                if (d < minDist) {
                    minDist = d;
                    color = colorPalette.get(paletteColor);
                }
            }

            if (color != null) {
                color.setCoverage(coverage);
                if (result.containsKey(color.getName())) {
                    Color existingColor = result.get(color.getName());
                    existingColor.setCoverage(
                            existingColor.getCoverage() + color.getCoverage());
                }
                else {
                    result.put(color.getName(), color);
                }
            }
        }

        List<Color> values = Lists.newArrayList(result.values());
        Collections.sort(values, (o1, o2) -> Floats.compare(o2.getCoverage(), o1.getCoverage()));

        for (Color color: values) {
            logger.debug("Mapped {} coverage: {}", color, color.getCoverage());
        }

        return values;
    }

    private List<Color> getColors(Map<int[], Float> clusters) {
        List<Color> result = Lists.newArrayListWithCapacity(clusters.size());
        for (Map.Entry<int[], Float> entry: clusters.entrySet()) {
                Color color = new Color();
                color.setBlue(entry.getKey()[0]);
                color.setGreen(entry.getKey()[1]);
                color.setRed(entry.getKey()[2]);
                color.setHex(String.format("#%02x%02x%02x",
                        color.getRed(), color.getGreen(), color.getBlue()));

                color.setCoverage(entry.getValue() * 100);
                result.add(color);
        }

        Collections.sort(result, (o1, o2) -> Floats.compare(o2.getCoverage(), o1.getCoverage()));
        for (Color color: result) {
            logger.debug("Original {} coverage: {}", color, color.getCoverage());
        }
        return result;
    }

    Map<String, Integer> COLORSPACES = ImmutableMap.of(
            "Lab", opencv_imgproc.COLOR_BGR2Lab,
            "HSV", opencv_imgproc.COLOR_BGR2HSV);

    @Override
    public void process(AssetBuilder asset) {

        ProxySchema proxies = asset.getAttr("proxies", ProxySchema.class);
        Proxy proxy = proxies.atLeastThisSize(128);

        if (proxy == null) {
            logger.debug("skipping color analysis, no proxy of suitable size");
        }

        String filePath = objectFileSystem.get(proxy.getName()).getFile().getAbsolutePath();
        opencv_core.Mat imgToAnalyze =  opencv_imgcodecs.imread(filePath);

        if (COLORSPACES.containsKey(colorSpace)) {
            opencv_imgproc.cvtColor(imgToAnalyze, imgToAnalyze, COLORSPACES.get(colorSpace));
        }

        Map<int[], Float> colorAnalysisResults = performVQAnalysis(imgToAnalyze);
        List<Color> mappedColors = getMappedColors(colorAnalysisResults);
        List<Color> originalColors = getColors(colorAnalysisResults);

        /**
         * TODO: Not 100% sure we need both here but we'll leave it as is.
         */
        asset.setAttr("colors.original", originalColors);
        asset.setAttr("colors.mapped", mappedColors);
    }
}
