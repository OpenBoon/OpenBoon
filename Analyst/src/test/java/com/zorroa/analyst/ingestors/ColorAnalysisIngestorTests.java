package com.zorroa.analyst.ingestors;

import com.zorroa.analyst.ingestors.ColorAnalysisIngestor;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotEquals;

/**
 * Created by barbara on 1/10/16.
 */
public class ColorAnalysisIngestorTests extends AssetBuilderTests {

    @Test
    public void testProcess() throws InterruptedException {

        // Set up and args passing/testing
        ColorAnalysisIngestor testIngestor = new ColorAnalysisIngestor();
        HashMap<String, Object> testIngestorArgs = new HashMap<String, Object>();
        /* String colorSpace = new String("HSV");
        testIngestorArgs.put("ColorSpace", colorSpace);
        testIngestor.setArgs(testIngestorArgs);*/
        setup(testIngestor);

        for (AssetBuilder asset : testAssets) {

            // Process the asset to compute histograms
            testIngestor.process(asset);
            Map<int[], Float> analysisResults = asset.getAttr("ColorAnalysis", "ColorClusters");
            Map<int[], String> colorMapping = asset.getAttr("ColorAnalysis", "ColorMapping");
            assertNotEquals(analysisResults, null);
            assertNotEquals(colorMapping, null);
        }
    }

}
