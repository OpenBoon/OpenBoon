package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zorroa.archivist.ArchivistConfiguration;
import com.zorroa.archivist.AssetExecutor;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.sdk.crawlers.AbstractCrawler;
import com.zorroa.archivist.sdk.crawlers.FileCrawler;
import com.zorroa.archivist.sdk.crawlers.HttpCrawler;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.filesystem.ObjectFileSystem;
import com.zorroa.archivist.security.BackgroundTaskAuthentication;
import com.zorroa.archivist.security.SecurityUtils;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

@Component
public class IngestExecutorServiceImpl implements IngestExecutorService {

    private static final Logger logger = LoggerFactory.getLogger(IngestExecutorServiceImpl.class);

    @Autowired
    Client client;

    @Autowired
    IngestService ingestService;

    @Autowired
    AssetDao assetDao;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    EventLogService eventLogService;

    @Autowired
    MessagingService messagingService;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    ObjectFileSystem objectFileSystem;

    @Value("${archivist.ingest.ingestWorkers}")
    private int ingestWorkerCount;

    private final ConcurrentMap<Integer, IngestWorker> runningIngests = Maps.newConcurrentMap();

    private Executor ingestExecutor;

    @PostConstruct
    public void init() {
        ingestExecutor = Executors.newFixedThreadPool(ingestWorkerCount);
    }

    @Override
    public boolean executeIngest(Ingest ingest) {
        return start(ingest, true /* reset counters */);
    }

    @Override
    public boolean resume(Ingest ingest) {
        return start(ingest, false /*don't reset counters*/);
    }

    protected boolean start(Ingest ingest, boolean firstStart) {
        IngestWorker worker = new IngestWorker(ingest, SecurityUtils.getUser());

        if (runningIngests.putIfAbsent(ingest.getId(), worker) == null) {

            if (firstStart) {
                // Reset counters and start time only on first execute, not restart
                ingestService.resetIngestCounters(ingest);
                ingestService.updateIngestStartTime(ingest, System.currentTimeMillis());
            }

            if (ArchivistConfiguration.unittest) {
                worker.run();
            } else {
                if (!ingestService.setIngestQueued(ingest))
                    return false;
                ingestExecutor.execute(worker);
            }
        } else {
            logger.warn("The ingest is already executing: {}", ingest);
        }

        return true;
    }

    @Override
    public boolean pause(Ingest ingest) {
        if (!shutdown(ingest)) {
            return false;
        }
        ingestService.setIngestPaused(ingest);
        return true;
    }

    @Override
    public boolean stop(Ingest ingest) {
        if (!shutdown(ingest)) {
            return false;
        }
        ingestService.setIngestIdle(ingest);
        return true;
    }

    protected boolean shutdown(Ingest ingest) {
        IngestWorker worker = runningIngests.get(ingest.getId());
        if (worker == null) {
            return false;
        }
        worker.shutdown();
        return true;
    }

    public class IngestWorker implements Runnable {

        private AssetExecutor assetExecutor;

        private LongAdder totalAssets = new LongAdder();

        private final Ingest ingest;

        private final User user;

        private boolean earlyShutdown = false;

        private Set<String> supportedFormats = Sets.newHashSet();

        private Set<String> skippedPaths;

        public IngestWorker(Ingest ingest, User user) {
            this.ingest = ingest;
            this.user = user;
            assetExecutor = new AssetExecutor(ingest.getAssetWorkerThreads());
        }

        public void shutdown() {
            earlyShutdown = true;           // Force cleanup at end of ingest
            assetExecutor.shutdownNow();
            try {
                while (!assetExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    Thread.sleep(250);
                }
            } catch (InterruptedException e) {
                logger.warn("Asset processing termination interrupted: " + e.getMessage());
            }
        }


        @Override
        public void run() {

            if (!ingestService.setIngestRunning(ingest)) {
                logger.warn("Unable to set ingest {} to the running state.", ingest);
                return;
            }

            IngestPipeline pipeline;

            try {

                try {
                    /**
                     * Re-authorize this thread with the same user using InternalAuthentication.  This will add
                     * the internal::server permission to their list of permissions.
                     */
                    SecurityContextHolder.getContext().setAuthentication(
                            authenticationManager.authenticate(new BackgroundTaskAuthentication(user)));

                    pipeline = ingestService.getIngestPipeline(ingest.getPipelineId());
                } catch (Exception e) {
                    /*
                     * Something went wrong setting up the ingestor classes.
                     */
                    logger.warn("Failed to setup the ingest pipeline, unexpected: {}", e.getMessage(), e);
                    eventLogService.log(ingest, "Failed to setup the ingest pipeline", e);
                    messagingService.broadcast(new Message(MessageType.INGEST_EXCEPTION, ingest));
                    return;
                }

                /*
                 * Figure out the skipped paths
                 */
                skippedPaths = ingestService.getSkippedPaths(ingest);
                for (String path : skippedPaths) {
                    eventLogService.log(ingest, "Skipping path due to previous failure: {}", path);
                }

                try {
                    walkIngestPaths(ingest, pipeline);
                } catch (Exception e) {
                    /*
                     * Something went wrong while walking the file system, however the asset
                     * threads might still be working so we don't want to jump right into
                     * the lower finally block, but wait until the asset threads are done.
                     */
                    eventLogService.log(ingest, "Failed to execute ingest on paths {}", e, ingest.getPaths());
                    messagingService.broadcast(new Message(MessageType.INGEST_EXCEPTION, ingest));
                }

            } finally {

                /*
                 * Force a refresh so the tear downs can see any recently added data.
                 */
                assetDao.refresh();

                /*
                 * Remove the current ingest from running ingests.
                 */
                runningIngests.remove(ingest.getId());

                /*
                 * Pull a new copy of the ingest with all updated fields.
                 */
                Ingest finishedIngest = ingestService.getIngest(ingest.getId());

                if (!earlyShutdown) {
                    ingestService.setIngestIdle(finishedIngest);

                    eventLogService.log(finishedIngest, "ingest finished , created {}, updated: {}, errors: {}",
                            finishedIngest.getCreatedCount(), finishedIngest.getUpdatedCount(), finishedIngest.getErrorCount());
                } else {
                    eventLogService.log(finishedIngest, "ingest was manually shut down, created {}, updated: {}, errors: {}",
                            finishedIngest.getCreatedCount(), finishedIngest.getUpdatedCount(), finishedIngest.getErrorCount());
                }
            }
        }

        /**
         * Walks the file paths specified on an ingest. When a valid asset is found its handed
         * to the asset processor threads.
         *
         * @param ingest
         * @param pipeline
         * @throws IOException
         */
        private void walkIngestPaths(Ingest ingest, IngestPipeline pipeline) throws IOException {

            Consumer<File> consumer = file -> {
                totalAssets.increment();
                assetExecutor.execute(() -> {
                    try {
                        /*
                         * TODO: Add analyst client communication here.
                         */
                    } catch (Exception e) {
                        logger.warn("Failed to analyze: {}", file, e);
                    }
                });
            };

            /**
             * For now this is a hard coded list, but we'll need to support plugins
             * for cralwers as well.
             */
            Map<String, AbstractCrawler> crawlers = ImmutableMap.of(
                    "file", new FileCrawler(objectFileSystem),
                    "http", new HttpCrawler(objectFileSystem));

            for (String path : ingest.getPaths()) {

                URI uri = URI.create(path);
                String type = uri.getScheme();
                if (type == null) {
                    type = "file";
                    uri = URI.create("file:" + path);
                }

                AbstractCrawler crawler = crawlers.get(type);
                if (crawler == null) {
                    eventLogService.log("No crawler class for type: '{}'", type);
                    continue;
                }

                crawler.setTargetFileFormats(supportedFormats);
                crawler.setIgnoredPaths(skippedPaths);
                crawler.start(uri, consumer);
            }
        }
    }
}
