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
import static org.junit.Assert.assertTrue;

/**
 * Created by wex on 7/6/15.
 */
public class AssetMetadataProcessorTests extends ArchivistApplicationTests {


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
                new IngestProcessorFactory("com.zorroa.archivist.processors.AssetMetadataProcessor", args));
        ingestPipelineDao.create(builder);

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()).setPipeline("test"));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex(1000);

        List<Asset> assets = assetDao.getAll();
        assertEquals(2, assets.size());
        Map<String, Object> sourceMap = (Map<String, Object>) assets.get(0).getDocument().get("source");
        assertTrue(sourceMap.containsKey("date"));
    }

}
