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

import static org.junit.Assert.assertNotNull;

/**
 * Created by chambers on 3/25/16.
 */
public class ShotgunIngestorTests extends AbstractTest {

    @Test
    public void testProcess() {

        FilePathIngestor.Options opts = new FilePathIngestor.Options();
        opts.representations = ImmutableList.of(new FilePathIngestor.RepresentationMatcher("blend"));

        final List<IngestProcessor> pipeline = ImmutableList.<IngestProcessor>builder()
                .add(initIngestProcessor(new FilePathIngestor().setOpts(opts)))
                .add(initIngestProcessor(new BlenderIngestor()))
                .add(initIngestProcessor(new ShotgunIngestor()))
                .add(initIngestProcessor(new ProxyProcessor()))
                .build();

        List<AssetBuilder> assets = Lists.newArrayList();
        for (File f : new File("src/test/resources/reprs").listFiles()) {
            try {
                assets.add(ingestFile(f, pipeline));
            } catch (SkipIngestException e) {
                // ignore
            }
        }
        assertNotNull(assets.get(0).getAttr("shotgun"));
    }

}
