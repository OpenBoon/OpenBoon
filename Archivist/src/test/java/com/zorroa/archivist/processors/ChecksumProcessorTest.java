package com.zorroa.archivist.processors;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.repository.IngestPipelineDao;
import com.zorroa.archivist.service.IngestExecutorService;
import com.zorroa.archivist.service.IngestService;
import org.elasticsearch.common.collect.Maps;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 7/3/15.
 */
public class ChecksumProcessorTest extends ArchivistApplicationTests {

    @Autowired
    IngestPipelineDao ingestPipelineDao;

    @Autowired
    IngestService ingestService;

    @Autowired
    IngestExecutorService ingestExecutorService;

    @Autowired
    AssetDao assetDao;

    @Test
    public void testProcess() throws InterruptedException {

        Map<String, Object> args = Maps.newHashMap();

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("test");
        builder.addToProcessors(
                new IngestProcessorFactory("com.zorroa.archivist.processors.ChecksumProcessor", args));
        IngestPipeline pipeline = ingestPipelineDao.create(builder);

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()).setPipelineId(pipeline.getId()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex(500);

        List<Asset> assets = assetDao.getAll();
        assertEquals(2, assets.size());

        for (Asset asset: assets) {
            String path = asset.getValue("source.path");
            String crc32 = asset.getValue("source.hash");

            if (path.contains("beer_kettle_01.jpg")) {
                assertEquals(crc32, "9faa728d41dfb2c9416fb7c7fc6ad77d");
            }

            if (path.contains("new_zealand_wellington_harbour.jpg")) {
                assertEquals(crc32, "306a7de6f3d2da7bb4cd540e2cbfba8e");
            }
        }
    }
}