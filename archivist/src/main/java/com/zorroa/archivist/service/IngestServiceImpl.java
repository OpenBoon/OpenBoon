package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.zorroa.archivist.domain.ImportSpec;
import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestSpec;
import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.repository.IngestDao;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.domain.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

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

    private final ThreadPoolTaskScheduler scheduler =  new ThreadPoolTaskScheduler();

    private final ConcurrentMap<Integer, ScheduledFuture> scheduled = Maps.newConcurrentMap();

    @PostConstruct
    public void init() {
        /**
         * Schedule all the current ingests.
         */
        scheduler.setPoolSize(4);
        for (Ingest i: ingestDao.getAll()) {
            if (!i.isAutomatic()) {
                continue;
            }
            schedule(i);
        }
    }

    @Override
    public Ingest create(IngestSpec spec) {
        Ingest i = ingestDao.create(spec);

        event.afterCommit(()-> {
            if (i.isAutomatic()) { schedule(i); }
            message.broadcast(new Message("INGEST_CREATE",
                    ImmutableMap.of("id", i.getId())));

        });

        if (spec.isRunNow()) {
            event.afterCommit(() -> {
                spawnImportJob(i);
            });
        }

        return i;
    }

    @Override
    public Job spawnImportJob(Ingest ingest) {
        try {
            ingestDao.refresh(ingest);
        } catch (EmptyResultDataAccessException e) {
            logger.warn("Ingest {} no longer exists.", ingest.getId());
            return null;
        }

        ImportSpec spec = new ImportSpec();
        spec.setPipelineId(ingest.getPipelineId());
        spec.setPipeline(ingest.getPipeline());
        spec.setGenerators(ingest.getGenerators());
        spec.setName("ingest " + ingest.getName());
        return importService.create(spec);
    }

    @Override
    public boolean update(int id, Ingest spec) {
        boolean result = ingestDao.update(id, spec);
        if (result) {
            event.afterCommit(() -> {
                cancel(id);
                schedule(ingestDao.get(id));
                message.broadcast(new Message("INGEST_UPDATE",
                        ImmutableMap.of("id", id)));
            });
        }
        return result;
    }

    @Override
    public boolean delete(int id) {
        boolean result = ingestDao.delete(id);
        if (result) {
            event.afterCommit(() -> {
                cancel(id);
                message.broadcast(new Message("INGEST_DELETE",
                        ImmutableMap.of("id", id)));
            });
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

    private void schedule(Ingest i) {
        ScheduledFuture future = scheduler.schedule(() -> {
            spawnImportJob(i);
        }, new CronTrigger(i.getSchedule().toString()));
        scheduled.put(i.getId(), future);
    }

    private void cancel(int id) {
        ScheduledFuture future = scheduled.get(id);
        if (future != null) {
            future.cancel(false);
        }
    }
}
