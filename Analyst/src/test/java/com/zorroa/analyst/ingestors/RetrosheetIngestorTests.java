/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.analyst.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import org.junit.Test;

public class RetrosheetIngestorTests extends AssetBuilderTests {

    @Test
    public void testProcess() throws InterruptedException {
        RetrosheetIngestor retro = new RetrosheetIngestor();
        setup(retro);
        for (AssetBuilder asset : testAssets) {
            retro.process(asset);
        }
    }
}
