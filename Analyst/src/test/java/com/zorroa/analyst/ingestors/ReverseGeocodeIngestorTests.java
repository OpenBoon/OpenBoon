package com.zorroa.analyst.ingestors;

import com.google.common.collect.ImmutableList;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.LocationSchema;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 4/14/16.
 */
public class ReverseGeocodeIngestorTests extends AbstractTest {

    @Test
    public void testProcess() {
        final List<IngestProcessor> pipeline = ImmutableList.<IngestProcessor>builder()
                .add(initIngestProcessor(new ImageIngestor()))
                .add(initIngestProcessor(new ReverseGeocodeIngestor()))
                .build();

        File file = getResourceFile("/images/faces.jpg");
        AssetBuilder asset = ingestFile(file, pipeline);

        LocationSchema schema = asset.getAttr("location", LocationSchema.class);
        assertEquals("Cát Bà", schema.getCity());
        assertEquals("VN", schema.getCountry());

        assertEquals("Cát Bà", asset.getAttr("location.city"));
        assertEquals("VN", asset.getAttr("location.country"));
    }
}
