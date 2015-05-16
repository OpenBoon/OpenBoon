package com.zorroa.archivist.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import com.zorroa.archivist.domain.IngestPipelineBuilder;
import com.zorroa.archivist.domain.IngestPipeline;
import com.zorroa.archivist.repository.IngestPipelineDao;

/**
 *
 * The ingest service is responsible for processing IngestRequest objects which
 * results in the creation of assets using the assetService.
 *
 * @author chambers
 *
 */
@Component
public class IngestServiceImpl implements IngestService {

    protected static final Logger logger = LoggerFactory.getLogger(IngestServiceImpl.class);

    @Autowired
    IngestPipelineDao ingestPipelineDao;

    @Autowired
    AsyncTaskExecutor ingestExecutor;

    public IngestPipeline createIngestPipeline(IngestPipelineBuilder builder) {
        String id = ingestPipelineDao.create(builder);
        return ingestPipelineDao.get(id);
    }
}
