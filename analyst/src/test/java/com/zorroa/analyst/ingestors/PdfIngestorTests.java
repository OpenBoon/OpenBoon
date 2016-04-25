package com.zorroa.analyst.ingestors;

import com.google.common.collect.ImmutableList;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.filesystem.ObjectFileSystem;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 3/31/16.
 */
public class PdfIngestorTests extends AbstractTest {

    @Autowired
    ObjectFileSystem objectFileSystem;

    @Test
    public void testProcess() {
        List<IngestProcessor> pipeline = ImmutableList.<IngestProcessor>builder()
                .add(initIngestProcessor(new PdfIngestor()))
                .build();

        File file = getResourceFile("/office/pdfTest.pdf");
        AssetBuilder asset = ingestFile(file, pipeline);
        assertEquals(10, asset.getLinks().getDerived().size());
    }
}
