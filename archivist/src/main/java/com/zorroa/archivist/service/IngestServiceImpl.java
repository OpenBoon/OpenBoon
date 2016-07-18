package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.domain.ImportSpec;
import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestSpec;
import com.zorroa.archivist.repository.IngestDao;
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
 * Created by chambers on 7/9/16.
 */
@Service
@Transactional
public class IngestServiceImpl implements IngestService {

    private static final Logger logger = LoggerFactory.getLogger(IngestServiceImpl.class);

    @Autowired
    IngestDao ingestDao;

    @Autowired
    MessagingService message;

    @Autowired
    TransactionEventManager event;

    @Autowired
    ImportService importService;

    @Override
    public Ingest create(IngestSpec spec) {
        Ingest i = ingestDao.create(spec);
        event.afterCommit(()->
                message.broadcast(new Message("INGEST_CREATE",
                        ImmutableMap.of("id", i.getId()))));

        if (spec.isRunNow()) {
            event.afterCommit(() -> {
                spawnImportJob(i);
            });
        }

        return i;
    }

    @Override
    public void spawnImportJob(Ingest ingest) {
        ImportSpec spec = new ImportSpec();
        spec.setPipelineId(ingest.getPipelineId());
        spec.setPipeline(ingest.getPipeline());
        spec.setGenerators(ingest.getGenerators());
        spec.setName("ingest-" + ingest.getName());
        importService.create(spec);
    }

    @Override
    public boolean update(int id, IngestSpec spec) {
        boolean result = ingestDao.update(id, spec);
        if (result) {
            event.afterCommit(() ->
                    message.broadcast(new Message("INGEST_UPDATE",
                            ImmutableMap.of("id", id))));
        }
        return result;
    }

    @Override
    public boolean delete(int id) {
        boolean result = ingestDao.delete(id);
        if (result) {
            event.afterCommit(() ->
                    message.broadcast(new Message("INGEST_DELETE",
                            ImmutableMap.of("id", id))));
        }
        return result;
    }

    @Override
    public List<Ingest> getAll() {
        return ingestDao.getAll();
    }

    @Override
    public PagedList<Ingest> getAll(Paging page) {
        return ingestDao.getAll(page);
    }

    @Override
    public Ingest get(int id) {
        return ingestDao.get(id);
    }

    @Override
    public Ingest get(String name) {
        return ingestDao.get(name);
    }

    @Override
    public long count() {
        return ingestDao.count();
    }

    @Override
    public boolean exists(String name) {
        return ingestDao.exists(name);
    }
}
