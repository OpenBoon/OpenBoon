package com.zorroa.analyst.ingestors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.exception.SkipIngestException;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.junit.Test;

import java.io.File;
import java.util.List;

/**
 * Created by chambers on 4/8/16.
 */
public class VideoIngestorTests extends AbstractTest {

    @Test
    public void testProcess() {


        final List<IngestProcessor> pipeline = ImmutableList.<IngestProcessor>builder()
                .add(initIngestProcessor(new VideoIngestor()))
                .add(initIngestProcessor(new ProxyProcessor()))
                .build();

        List<AssetBuilder> assets = Lists.newArrayList();
        for (File f : new File("src/test/resources/video").listFiles()) {
            try {
                assets.add(ingestFile(f, pipeline));
            } catch (SkipIngestException e) {
                // ignore
            }
        }
    }
}
