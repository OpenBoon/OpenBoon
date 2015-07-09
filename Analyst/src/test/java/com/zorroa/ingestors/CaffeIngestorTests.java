package com.zorroa.ingestors;

import com.zorroa.archivist.sdk.AssetBuilder;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class CaffeIngestorTests extends AssetBuilderTests {

    @Test
    public void testProcess() throws InterruptedException {
        CaffeIngestor caffe = new CaffeIngestor();
        for (AssetBuilder asset : testAssets) {
            caffe.process(asset);
        }
    }

}
