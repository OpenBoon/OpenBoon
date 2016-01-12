package com.zorroa.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import org.junit.Test;

public class CaffeIngestorTests extends AssetBuilderTests {

    @Test
    public void testProcess() {

        CaffeIngestor caffe = new CaffeIngestor();
        setup(caffe);
        for (AssetBuilder asset : testAssets) {
            caffe.process(asset);
        }
    }

}
