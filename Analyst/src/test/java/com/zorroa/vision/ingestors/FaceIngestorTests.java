package com.zorroa.vision.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.vision.AssetBuilderTests;
import com.zorroa.vision.ingestors.FaceIngestor;
import org.junit.Test;

public class FaceIngestorTests extends AssetBuilderTests {

    @Test
    public void testProcess() throws InterruptedException {
        FaceIngestor face = new FaceIngestor();
        setup(face);
        for (AssetBuilder asset : testAssets) {
            face.process(asset);
        }
    }

}
