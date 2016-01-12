package com.zorroa.vision.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.vision.AssetBuilderTests;
import com.zorroa.vision.ingestors.LogoIngestor;
import org.junit.Test;

public class LogoIngestorTests extends AssetBuilderTests {

    @Test
    public void testProcess() throws InterruptedException {
        LogoIngestor logo = new LogoIngestor();
        setup(logo);
        for (AssetBuilder asset : testAssets) {
            logo.process(asset);
        }
    }

}
