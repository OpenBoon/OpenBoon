package com.zorroa.archivist.ingestors;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.repository.IngestPipelineDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.schema.ImageSchema;
import com.zorroa.archivist.sdk.schema.UserSchema;
import com.zorroa.archivist.sdk.service.IngestService;
import com.zorroa.archivist.service.IngestExecutorService;
import org.elasticsearch.common.collect.Maps;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by wex on 7/6/15.
 */
public class ImageIngestorTests extends ArchivistApplicationTests {


    @Autowired
    IngestPipelineDao ingestPipelineDao;

    @Autowired
    IngestService ingestService;

    @Autowired
    IngestExecutorService ingestExecutorService;

    @Autowired
    AssetDao assetDao;

    @Test
    public void testProcessImage() throws InterruptedException {

        Map<String, Object> args = Maps.newHashMap();

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("image");
        builder.addToProcessors(
                new ProcessorFactory<>(ImageIngestor.class));
        IngestPipeline pipeline = ingestPipelineDao.create(builder);

        Ingest ingest = ingestService.createIngest(
                new IngestBuilder(getStaticImagePath("agg"))
                        .setName("ImageIngestTest")
                        .setPipelineId(pipeline.getId()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex();

        List<Asset> assets = assetDao.getAll();
        assertEquals(3, assets.size());

        for (Asset asset: assets) {
            ImageSchema schema = assets.get(0).getSchema("image", ImageSchema.class);
            assertTrue(schema.getWidth() > 0);
            assertTrue(schema.getHeight() > 0);

            /**
             * Check we have a rating.
             */
            if (asset.getSource().getBasename().contains("painted_canyon")) {
                assertEquals(4, (int) asset.getSchema("user", UserSchema.class).getRating());
            }
        }
    }
}
