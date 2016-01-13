package com.zorroa.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 1/12/16.
 */
public class OilWellLogIngestorTests {

    private static final Logger logger = LoggerFactory.getLogger(OilWellLogIngestorTests.class);

    @Test
    public void testProcess() {

        OilWellLogIngestor ingestor = new OilWellLogIngestor();
        ingestor.init();

        File dir = new File("src/test/resources/oil/well_logs");
        for (File file: dir.listFiles()) {
            if (!file.isFile()) {
                continue;
            }

            AssetBuilder asset = new AssetBuilder(file.getAbsolutePath());
            try {
                ingestor.process(asset);
                assertEquals(asset.getBasename(), asset.getAttr("wellLog", "type"));
            } catch (Exception e) {
                logger.warn("Failed to ingest: {}", e);
            }
        }
    }
}
