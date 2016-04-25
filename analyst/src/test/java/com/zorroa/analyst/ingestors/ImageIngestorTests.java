package com.zorroa.analyst.ingestors;

import com.google.common.collect.ImmutableList;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.ImageSchema;
import com.zorroa.archivist.sdk.util.Json;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by chambers on 4/15/16.
 */
public class ImageIngestorTests extends AbstractTest {

    @Test
    public void testProcessCreation() {
        List<IngestProcessor> pipeline = ImmutableList.<IngestProcessor>builder()
                .add(initIngestProcessor(new ImageIngestor()))
                .build();

        File file = getResourceFile("/images/toucan.jpg");
        AssetBuilder asset = ingestFile(file, pipeline);

        ImageSchema imageSchema = asset.getAttr("image", ImageSchema.class);
        assertNotNull(imageSchema);

        assertEquals(512, (int) imageSchema.getWidth());
        assertEquals(341, (int) imageSchema.getHeight());

        assertTrue(asset.attrExists("image.Exif"));
        assertTrue(asset.attrExists("image.JFIF"));
        assertTrue(asset.attrExists("image.JPEG"));
        assertTrue(asset.attrExists("image.IPTC"));

        logger.info(Json.prettyString(asset.getDocument()));
    }

    @Test
    public void testProcessUpdate() {
        List<IngestProcessor> pipeline = ImmutableList.<IngestProcessor>builder()
                .add(initIngestProcessor(new ImageIngestor()))
                .build();

        File file = getResourceFile("/images/toucan.jpg");
        ingestFile(file, pipeline);
        AssetBuilder asset = ingestFile(file, pipeline);

        ImageSchema imageSchema = asset.getAttr("image", ImageSchema.class);
        assertNotNull(imageSchema);

        assertEquals(512, (int) imageSchema.getWidth());
        assertEquals(341, (int) imageSchema.getHeight());

        assertTrue(asset.attrExists("image.Exif"));
        assertTrue(asset.attrExists("image.JFIF"));
        assertTrue(asset.attrExists("image.JPEG"));
        assertTrue(asset.attrExists("image.IPTC"));

        logger.info(Json.prettyString(asset.getDocument()));
    }
}
