package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.domain.Pipeline;
import com.zorroa.archivist.domain.PipelineSpec;
import com.zorroa.archivist.repository.PipelineDao;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.domain.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 *
 * The ingest service is responsible for processing IngestRequest objects which
 * results in the creation of assets using the assetService.
 *
 * @author chambers
 *
 */
@Service
@Transactional
public class PipelineServiceImpl implements PipelineService {

    private static final Logger logger = LoggerFactory.getLogger(PipelineServiceImpl.class);

    @Autowired
    PipelineDao pipelineDao;

    @Autowired
    MessagingService message;

    @Autowired
    TransactionEventManager event;

    @Override
    public Pipeline create(PipelineSpec spec) {
        Pipeline p = pipelineDao.create(spec);
        event.afterCommit(()->
                message.broadcast(new Message("PIPELINE_CREATE",
                        ImmutableMap.of("id", p.getId()))));
        return p;
    }

    @Override
    public Pipeline get(int id) {
        return pipelineDao.get(id);
    }

    @Override
    public Pipeline get(String name) {
        return pipelineDao.get(name);
    }

    @Override
    public boolean exists(String name) {
        return pipelineDao.exists(name);
    }

    @Override
    public List<Pipeline> getAll() {
        return pipelineDao.getAll();
    }

    @Override
    public PagedList<Pipeline> getAll(Paging page) {
        return pipelineDao.getAll(page);
    }

    @Override
    public boolean update(int id, Pipeline spec) {
        boolean result = pipelineDao.update(id, spec);
        if (result) {
            event.afterCommit(() ->
                    message.broadcast(new Message("PIPELINE_UPDATE",
                            ImmutableMap.of("id", id))));
        }
        return result;
    }

    @Override
    public boolean delete(int id) {
        boolean result = pipelineDao.delete(id);
        if (result) {
            event.afterCommit(() ->
                    message.broadcast(new Message("PIPELINE_DELETE",
                            ImmutableMap.of("id", id))));
        }
        return result;
    }
}
