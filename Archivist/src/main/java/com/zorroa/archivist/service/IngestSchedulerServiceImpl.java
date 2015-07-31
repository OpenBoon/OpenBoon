package com.zorroa.archivist.service;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.zorroa.archivist.ArchivistConfiguration;
import com.zorroa.archivist.FileUtils;
import com.zorroa.archivist.IngestException;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.sdk.AssetBuilder;
import com.zorroa.archivist.sdk.IngestProcessor;
import com.zorroa.archivist.sdk.IngestProcessorService;
import org.elasticsearch.common.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class IngestSchedulerServiceImpl extends AbstractScheduledService implements IngestSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(IngestServiceImpl.class);

    @Autowired
    IngestService ingestService;

    @Autowired
    IngestProcessorService ingestProcessorService;

    @Autowired
    ImageService imageService;

    @Autowired
    AssetService assetService;

    @Autowired
    ApplicationContext applicationContext;

    @Value("${archivist.ingest.ingestWorkers}")
    private int ingestWorkerCount;

    @Value("${archivist.ingest.assetWorkersPerIngest}")
    private int assetWorkerCount;
    
    private final AtomicInteger runningIngestCount = new AtomicInteger();

    private final ConcurrentMap<Long, IngestWorker> runningIngests = Maps.newConcurrentMap();

    private Executor ingestExecutor;

    @PostConstruct
    public void init() {

        if (ArchivistConfiguration.unittest) {
            ingestExecutor = new SyncTaskExecutor();
        }
        else {
            ingestExecutor = Executors.newFixedThreadPool(ingestWorkerCount);
        }
        startAsync();
    }

    @Override
    protected void runOneIteration() throws Exception {
        if (!ArchivistConfiguration.unittest) {
            executeNextIngest();
        }
    }

    @Override
    protected Scheduler scheduler() {
        /**
         * Check for new ingests
         */
       return Scheduler.newFixedRateSchedule(0, 2, TimeUnit.SECONDS);
    }

    @Override
    public Ingest executeNextIngest() {
        if (runningIngestCount.get() >= ingestWorkerCount) {
            return null;
        }

        List<Ingest> ingests = ingestService.getAllIngests(IngestState.Queued, 1);
        if (ingests.isEmpty()) {
            return null;
        }

        executeIngest(ingests.get(0));
        return ingests.get(0);
    }


    @Override
    public void executeIngest(Ingest ingest) {
        IngestWorker worker = new IngestWorker(ingest, assetWorkerCount);
        applicationContext.getAutowireCapableBeanFactory().autowireBean(worker);

        if (runningIngests.putIfAbsent(ingest.getId(), worker) == null) {
            ingestExecutor.execute(worker);
        }
        else {
            logger.warn("The ingest is already executing: {}", ingest);
        }
    }
}
