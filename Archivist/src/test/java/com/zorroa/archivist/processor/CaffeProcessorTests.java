package com.zorroa.archivist.processor;

import static org.junit.Assert.*;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.repository.IngestPipelineDao;
import com.zorroa.archivist.service.IngestSchedulerService;
import com.zorroa.archivist.service.IngestService;
import org.elasticsearch.common.collect.Maps;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CaffeProcessorTests extends ArchivistApplicationTests {

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
                new IngestProcessorFactory("com.zorroa.archivist.processors.CaffeProcessor", args));
        ingestPipelineDao.create(builder);

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()).setPipeline("test"));
        ingestSchedulerService.executeIngest(ingest);
        refreshIndex();

        List<Asset> assets = assetDao.getAll();
        assertTrue(((String)((ArrayList)((LinkedHashMap) assets.get(0).getDocument().get("caffe")).get("keywords")).get(0)).equals("lakeside"));
        assertTrue(((String)((ArrayList)((LinkedHashMap) assets.get(1).getDocument().get("caffe")).get("keywords")).get(0)).equals("barber chair"));
    }
}
