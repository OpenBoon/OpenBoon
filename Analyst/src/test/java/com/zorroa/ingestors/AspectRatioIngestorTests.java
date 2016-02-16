package com.zorroa.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import org.junit.Test;

/**
 * Created by jbuhler on 2/16/16.
 */
public class AspectRatioIngestorTests  extends AssetBuilderTests {
    @Test
    public void testProcess() throws InterruptedException {
        AspectRatioIngestor aspect = new AspectRatioIngestor();
        setup(aspect);
        for (AssetBuilder asset : testAssets) {
            aspect.process(asset);
        }
    }
}


