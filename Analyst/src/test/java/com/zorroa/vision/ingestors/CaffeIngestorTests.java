package com.zorroa.vision.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.vision.AssetBuilderTests;
import com.zorroa.vision.ingestors.CaffeIngestor;
import org.junit.Test;

public class CaffeIngestorTests extends AssetBuilderTests {

    @Test
    public void testProcess() throws InterruptedException {

        CaffeIngestor caffe = new CaffeIngestor();
        setup(caffe);
        for (AssetBuilder asset : testAssets) {
            caffe.process(asset);
        }
    }

}
