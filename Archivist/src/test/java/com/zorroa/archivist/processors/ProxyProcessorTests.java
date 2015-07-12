package com.zorroa.archivist.processors;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.repository.IngestPipelineDao;
import com.zorroa.archivist.service.IngestSchedulerService;
import com.zorroa.archivist.service.IngestService;
import org.elasticsearch.common.collect.Maps;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProxyProcessorTests extends ArchivistApplicationTests {

    @Autowired
    IngestPipelineDao ingestPipelineDao;

    @Autowired
    IngestService ingestService;

    @Autowired
    IngestSchedulerService ingestSchedulerService;

    @Autowired
    AssetDao assetDao;

    @Test
    public void testProcess() throws InterruptedException {

        Map<String, Object> args = Maps.newHashMap();

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("test");
        builder.addToProcessors(
                new IngestProcessorFactory("com.zorroa.archivist.processors.ProxyProcessor", args));
        ingestPipelineDao.create(builder);

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()).setPipeline("test"));
        ingestSchedulerService.executeIngest(ingest);
        refreshIndex(1000);

        List<Asset> assets = assetDao.getAll();
        assertEquals(2, assets.size());
        assertTrue(assets.get(0).getDocument().containsKey("tinyProxy"));
    }

}
