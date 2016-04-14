package com.zorroa.analyst.ingestors;

import com.google.common.collect.ImmutableList;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.Color;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.util.Json;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertNotEquals;

/**
 * Created by barbara on 1/10/16.
 */
public class ColorAnalysisIngestorTests extends AbstractTest {

    @Test
    public void testProcess() {
        final List<IngestProcessor> pipeline = ImmutableList.<IngestProcessor>builder()
                .add(initIngestProcessor(new ImageIngestor()))
                .add(initIngestProcessor(new ProxyProcessor()))
                .add(initIngestProcessor(new ColorAnalysisIngestor()))
                .build();

        AssetBuilder asset = ingestFile(new File("src/test/resources/images/faces.jpg"), pipeline);
        List<Color> originalColor = asset.getAttr("colors.original");
        List<Color> mappedColor = asset.getAttr("colors.mapped");
        assertNotEquals(originalColor, null);
        assertNotEquals(mappedColor, null);

        logger.info("{}", (Collection) asset.getKeywords().getAll());

        logger.info(Json.serializeToString(asset.getKeywords()));


    }
}
