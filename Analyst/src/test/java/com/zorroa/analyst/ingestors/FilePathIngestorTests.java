package com.zorroa.analyst.ingestors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.exception.SkipIngestException;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 3/21/16.
 */
public class FilePathIngestorTests extends AbstractTest {

    @Test
    public void testProcess() {

        ProcessorFactory<IngestProcessor> factory = new ProcessorFactory<>(FilePathIngestor.class,
                ImmutableMap.of("matchers", ImmutableList.of(
                        ImmutableMap.of("regex", "^.+/(.+?)\\.([^.]*$|$)",
                                "attrs", ImmutableList.of("foo:name", "foo:ext")))));

        IngestProcessor fp = factory.newInstance();
        fp.init();

        AssetBuilder asset = new AssetBuilder(getResourceFile("/images/toucan.jpg"));
        fp.process(asset);

        assertEquals("toucan", asset.getAttr("foo:name"));
        assertEquals("jpg", asset.getAttr("foo:ext"));
    }

    @Test
    public void testProcessNoArgs() {

        ProcessorFactory<IngestProcessor> factory = new ProcessorFactory<>(FilePathIngestor.class);

        IngestProcessor fp = factory.newInstance();
        fp.init();

        AssetBuilder asset = new AssetBuilder(getResourceFile("/images/toucan.jpg"));
        fp.process(asset);

        assertTrue(asset.getKeywords().getAll().contains("toucan.jpg"));
    }

    @Test
    public void testProcessSecondaryRepresentations() {

        ProcessorFactory<IngestProcessor> factory = new ProcessorFactory<>(
                FilePathIngestor.class, ImmutableMap.of("representations",
                    ImmutableList.of(ImmutableMap.of("primary","blend"))));

        IngestProcessor fp = factory.newInstance();
        fp.init();

        AssetBuilder asset = new AssetBuilder(getResourceFile("/reprs/butterfly.blend"));
        fp.process(asset);
    }

    @Test(expected= SkipIngestException.class)
    public void testSkipSecondaryRepresentations() {

        ProcessorFactory<IngestProcessor> factory = new ProcessorFactory<>(
                FilePathIngestor.class, ImmutableMap.of("representations",
                ImmutableList.of(ImmutableMap.of("primary","blend"))));

        IngestProcessor fp = factory.newInstance();
        fp.init();

        AssetBuilder asset = new AssetBuilder(getResourceFile("/reprs/butterfly.jpg"));
        fp.process(asset);
    }
}
