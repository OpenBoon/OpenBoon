package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zorroa.archivist.ArchivistConfiguration;
import com.zorroa.archivist.AssetExecutor;
import com.zorroa.archivist.aggregators.Aggregator;
import com.zorroa.archivist.aggregators.DateAggregator;
import com.zorroa.archivist.aggregators.IngestPathAggregator;
import com.zorroa.archivist.domain.UnitTestProcessor;
import com.zorroa.archivist.sdk.client.ClientException;
import com.zorroa.archivist.sdk.client.analyst.AnalystClient;
import com.zorroa.archivist.sdk.crawlers.AbstractCrawler;
import com.zorroa.archivist.sdk.crawlers.FileCrawler;
import com.zorroa.archivist.sdk.crawlers.HttpCrawler;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.exception.AnalystException;
import com.zorroa.archivist.sdk.filesystem.ObjectFileSystem;
import com.zorroa.archivist.security.BackgroundTaskAuthentication;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.common.service.EventLogService;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
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
    ApplicationProperties applicationProperties;

    @Autowired
    EventLogService eventLogService;

    @Autowired
    MessagingService messagingService;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    ObjectFileSystem objectFileSystem;

    @Autowired
    AnalystService analystService;

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

    @Override
    public List<Aggregator> getAggregators(Ingest ingest) {
        List<Aggregator> aggregators = Lists.newArrayList();
        aggregators.add(new DateAggregator());
        aggregators.add(new IngestPathAggregator());
        AutowireCapableBeanFactory autowire = applicationContext.getAutowireCapableBeanFactory();
        for (Aggregator aggregator : aggregators) {
            autowire.autowireBean(aggregator);
            aggregator.init(ingest);
        }
        return aggregators;
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

        private Timer aggregationTimer;

        private List<Aggregator> aggregators;

        public IngestWorker(Ingest ingest, User user) {
            this.ingest = ingest;
            this.user = user;
            this.assetExecutor = new AssetExecutor(
                    applicationProperties.max("archivist.ingest.defaultAssetWorkers", ingest.getAssetWorkerThreads()));
            aggregators = getAggregators(ingest);
            startAggregators();

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

        public void startAggregators() {
            aggregationTimer = new Timer(true);
            aggregationTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (!runningIngests.isEmpty()) {
                        for (Aggregator aggregator : aggregators) {
                            aggregator.aggregate();
                        }
                    }
                }
            }, 10000, 10000);
        }

        @Override
        public void run() {

            if (!ingestService.setIngestRunning(ingest)) {
                logger.warn("Unable to set ingest {} to the running state.", ingest);
                return;
            }

            try {

                SecurityContextHolder.getContext().setAuthentication(
                        authenticationManager.authenticate(new BackgroundTaskAuthentication(user)));
                IngestPipeline pipeline = ingestService.getIngestPipeline(ingest.getPipelineId());

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
                    eventLogService.log(ingest, "Failed to execute ingest on uris {}", e, ingest.getUris());
                    messagingService.broadcast(new Message(MessageType.INGEST_EXCEPTION, ingest));
                }

                assetExecutor.waitForCompletion();

            } finally {

                aggregationTimer.cancel();
                /*
                 * Force a refresh so the tear downs can see any recently added data.
                 */
                assetDao.refresh();
                if (aggregators != null) {
                    for (Aggregator aggregator : aggregators) {
                        aggregator.aggregate();
                    }
                }

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

        private void analyze(AnalystClient analysts, AnalyzeRequest req) {
            assetExecutor.execute(() -> {

                if (ArchivistConfiguration.unittest) {
                    for (String path: req.getPaths()) {
                        AssetBuilder a = new AssetBuilder(path);
                        UnitTestProcessor p = new UnitTestProcessor();
                        p.process(a);
                        assetDao.upsert(a);
                    }
                }
                else {
                    try {
                        /**
                         * This call is synchronous...otherwise we can't really
                         * keep track of when the ingest is done.  However, the
                         * data might not be in elastic when this returns.
                         */
                        AnalyzeResult result =  analysts.analyze(req);
                        ingestService.incrementIngestCounters(ingest,
                                result.created, result.updated, result.warnings, result.errors);
                        if (result.errors > 0) {
                            messagingService.broadcast(new Message(MessageType.INGEST_EXCEPTION, ingest));
                        }

                    } catch (ClientException e) {
                        /**
                         * This catch block is for handling the case where the AnalystClient
                         * cannot find any hosts to contact.  They are either down,  or rejecting
                         * work for some reason.
                         */
                        ingestService.incrementIngestCounters(ingest, 0, 0, 0, req.getPaths().size());
                        eventLogService.log(ingest, "Failed to contact analyst for processing ingest,", e);
                        messagingService.broadcast(new Message(MessageType.INGEST_EXCEPTION, ingest));
                    } catch (AnalystException e) {
                        /**
                         * This catch block is for handling the case where the Analyst fails
                         * to init or execute the pipeline.
                         */
                        ingestService.incrementIngestCounters(ingest, 0, 0, 0, req.getPaths().size());
                        eventLogService.log(ingest, "Failed to setup the ingest pipeline", e);
                        messagingService.broadcast(new Message(MessageType.INGEST_EXCEPTION, ingest));
                    }
                }
            });
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

            AnalystClient analyst;
            try {
                analyst = analystService.getAnalystClient();
            }
            catch (Exception e) {
                eventLogService.log(ingest, "Failed to find available analysts.");
                return;
            }

            List<String> paths = Lists.newArrayListWithCapacity(50);

            Consumer<File> consumer = file -> {
                totalAssets.increment();
                paths.add(file.getAbsolutePath());

                if (paths.size() < 10) {
                    return;
                }

                List<String> copyOfPaths = Lists.newArrayList(paths);
                paths.clear();

                analyze(analyst, new AnalyzeRequest()
                        .setUser(user.getUsername())
                        .setIngestId(ingest.getId())
                        .setIngestPipelineId(ingest.getPipelineId())
                        .setPaths(copyOfPaths)
                        .setProcessors(pipeline.getProcessors()));
            };

            /**
             * For now this is a hard coded list, but we'll need to support plugins
             * for cralwers as well.
             */
            Map<String, AbstractCrawler> crawlers = ImmutableMap.of(
                    "file", new FileCrawler(objectFileSystem),
                    "http", new HttpCrawler(objectFileSystem));

            for (String u : ingest.getUris()) {

                URI uri = URI.create(u);
                String type = uri.getScheme();
                if (type == null) {
                    type = "file";
                    uri = URI.create("file:" + u);
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

            if (!paths.isEmpty()) {
                analyze(analyst, new AnalyzeRequest()
                        .setUser(user.getUsername())
                        .setIngestId(ingest.getId())
                        .setIngestPipelineId(ingest.getPipelineId())
                        .setPaths(paths)
                        .setProcessors(pipeline.getProcessors()));
            }
        }
    }
}
