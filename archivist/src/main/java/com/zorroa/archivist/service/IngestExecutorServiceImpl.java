package com.zorroa.archivist.service;

import com.google.common.collect.*;
import com.zorroa.archivist.AnalyzeExecutor;
import com.zorroa.archivist.ArchivistConfiguration;
import com.zorroa.archivist.crawlers.FileCrawler;
import com.zorroa.archivist.crawlers.FlickrCrawler;
import com.zorroa.archivist.crawlers.HttpCrawler;
import com.zorroa.archivist.domain.UnitTestProcessor;
import com.zorroa.archivist.security.BackgroundTaskAuthentication;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.common.service.EventLogService;
import com.zorroa.sdk.client.analyst.AnalystClient;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.exception.AbortCrawlerException;
import com.zorroa.sdk.exception.ArchivistException;
import com.zorroa.sdk.processor.Crawler;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
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
    AnalystService analystService;

    @Autowired
    HealthEndpoint healthEndPoint;

    @Autowired
    AggregationService aggregationService;

    @Value("${archivist.ingest.maxRunningIngests}")
    private int maxRunningIngests;

    @Value("${archivist.ingest.defaultWorkerThreads}")
    private int defaultWorkerThreads;

    @Value("${archivist.ingest.batchSize}")
    private int batchSize;

    private final ConcurrentMap<Integer, IngestWorker> runningIngests = Maps.newConcurrentMap();

    private Executor ingestExecutor;

    @PostConstruct
    public void init() {
        ingestExecutor = Executors.newFixedThreadPool(maxRunningIngests);
    }

    @Override
    public boolean start(Ingest ingest) {
        if (!isHealthy()) {
            eventLogService.log(ingest, "Could not start ingest {}, Zorroa cluster healthy.", ingest);
            return false;
        }

        IngestWorker worker = new IngestWorker(ingest, SecurityUtils.getUser());
        if (runningIngests.putIfAbsent(ingest.getId(), worker) == null) {
                ingestService.resetIngestCounters(ingest);
                ingestService.updateIngestStartTime(ingest, System.currentTimeMillis());

            if (ArchivistConfiguration.unittest) {
                worker.run();
            } else {
                if (!ingestService.setIngestQueued(ingest)) {
                    return false;
                }
                ingestExecutor.execute(worker);
            }
        } else {
            logger.warn("The ingest is already executing: {}", ingest);
        }

        return true;
    }

    @Override
    public boolean pause(Ingest ingest) {
        if (ingestService.setIngestPaused(ingest, true)) {
            IngestWorker worker = runningIngests.get(ingest.getId());
            if (worker != null) {
                worker.pause(true);
            }
            eventLogService.log(ingest, "Ingest '{}' paused.", ingest.getName());
            return true;
        }
        return false;
    }

    @Override
    public boolean resume(Ingest ingest) {
        if (ingestService.setIngestPaused(ingest, false)) {
            IngestWorker worker = runningIngests.get(ingest.getId());
            if (worker != null) {
                aggregationService.invalidate(ingest);
                worker.pause(false);
                synchronized (worker) {
                    worker.notify();
                }
            }

            eventLogService.log(ingest, "Ingest '{}' resumed.", ingest.getName());
        }

        return false;
    }

    @Override
    public boolean stop(Ingest ingest) {
        if (ingestService.setIngestIdle(ingest)) {
            IngestWorker worker = runningIngests.get(ingest.getId());
            if (worker == null) {
                return false;
            }
            worker.shutdown();
            synchronized(worker) {
                worker.notify();
            }

            eventLogService.log(ingest, "Ingest '{}' stopped.", ingest.getName());
            return true;
        }

        return false;
    }

    private boolean isHealthy() {
        return healthEndPoint.invoke().getStatus() == Status.UP;
    }

    public class IngestWorker implements Runnable {

        private LongAdder totalAssets = new LongAdder();

        private final Ingest ingest;

        private final User user;

        private AtomicBoolean earlyShutdown = new AtomicBoolean(false);

        private AtomicBoolean paused = new AtomicBoolean(false);

        private Set<String> supportedFormats = Sets.newHashSet();

        private BlockingQueue<AnalyzeRequestEntry> fileQueue = Queues.newLinkedBlockingQueue();

        private AnalyzeExecutor analyzeExecutor = new AnalyzeExecutor(1);

        public IngestWorker(Ingest ingest, User user) {
            this.ingest = ingest;
            this.user = user;
        }

        public boolean shutdown() {
            boolean stateChanged =  earlyShutdown.compareAndSet(false, true);
            if (stateChanged) {
                analyzeExecutor.shutdownNow();
            }
            return stateChanged;
        }

        public boolean pause(boolean value) {
            return paused.compareAndSet(!value, value);
        }

        public void checkStateChange() {
            if (earlyShutdown.get()) {
                throw new AbortCrawlerException("Early ingest shutdown.");
            }

            if (paused.get()) {
                try {
                    synchronized(this) {
                        this.wait();
                    }
                } catch (InterruptedException e) {
                    logger.warn("Paused crawler thread interrupted");
                }
            }
        }

        @Override
        public void run() {

            if (!ingestService.setIngestRunning(ingest)) {
                logger.warn("Unable to set ingest {} to the running state.", ingest);
                return;
            }

            eventLogService.log(ingest, "Ingest '{}' started.", ingest.getName());

            try {

                SecurityContextHolder.getContext().setAuthentication(
                        authenticationManager.authenticate(new BackgroundTaskAuthentication(user)));
                IngestPipeline pipeline = ingestService.getIngestPipeline(ingest.getPipelineId());

                try {
                    aggregationService.invalidate(ingest);
                    walkIngestPaths(ingest, pipeline);
                    eventLogService.log(ingest, "Total assets detected {}, batches remaining {}",
                            totalAssets.longValue(), analyzeExecutor.size());
                    ingestService.setTotalAssetCount(ingest, totalAssets.longValue());

                } catch (Exception e) {
                    earlyShutdown.set(true);

                    /**
                     * If a crawler failure occurs, log it and then process the rest
                     * of the analyze queue.
                     */
                    eventLogService.log(ingest, "Ingest init failure {}", e, e.getMessage());
                    messagingService.broadcast(new Message(MessageType.INGEST_EXCEPTION, ingest));
                }

                if (analyzeExecutor.size() != 0) {
                    analyzeExecutor.waitForCompletion();
                    for (; ; ) {

                        if (earlyShutdown.get()) {
                            break;
                        }

                        Ingest rIngest = ingestService.getIngest(ingest.getId());
                        if (rIngest.getPendingCount() == 0) {
                            break;
                        }

                        try {
                            Thread.sleep(5000L);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }

            } finally {
                /*
                 * Note: a runtime exception may mean some of these variables
                 * are not initialized, so check for null. However, take care to
                 * initialize them to a sane default if possible.
                 */

                /*
                 * Remove the current ingest from running ingests.
                 */
                runningIngests.remove(ingest.getId());

                /*
                 * Pull a new copy of the ingest with all updated fields.
                 */
                Ingest finishedIngest = ingestService.getIngest(ingest.getId());
                ingestService.setIngestIdle(finishedIngest);

                /**
                 * These can be inaccurate during manual shutdown now, probably need it
                 * iterate all analysts and cancel requests for the ingest.
                 */
                if (!earlyShutdown.get()) {
                    eventLogService.log(finishedIngest, "Ingest finished , created {}, updated: {}, errors: {}",
                            finishedIngest.getCreatedCount(), finishedIngest.getUpdatedCount(), finishedIngest.getErrorCount());
                } else {
                    eventLogService.log(finishedIngest, "Ingest was manually shut down, created {}, updated: {}, errors: {}",
                            finishedIngest.getCreatedCount(), finishedIngest.getUpdatedCount(), finishedIngest.getErrorCount());
                }
            }
        }

        private void analyze(AnalystClient analysts, AnalyzeRequest req) {

            if (ArchivistConfiguration.unittest) {
                for (AnalyzeRequestEntry asset: req.getAssets()) {
                    AssetBuilder a = new AssetBuilder(asset.getUri());
                    UnitTestProcessor p = new UnitTestProcessor();
                    p.process(a);
                    assetDao.upsert(a);
                }
            }
            else {
                for(;;) {
                    try {
                        /**
                         * Check for paused or stopped.
                         */
                        checkStateChange();

                        /**
                         * Does not return a result.  If this throws, its only via
                         * a communication error and we'll try to recover from it.
                         */
                        analysts.analyzeAsync(req);
                        return;

                    } catch(AbortCrawlerException abort) {
                        // logged elsewhere
                        return;

                    } catch (Exception e) {
                        /**
                         * This catch block is for handling the case where communication to
                         * the analysts fail.
                         */
                        eventLogService.log(ingest, "Failed to contact analyst for processing ingest,", e);
                    }

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        //ignore
                    }
                }
            }
        }

        public void submitBatch(IngestPipeline pipeline) {

            List<AnalyzeRequestEntry> batch = Lists.newArrayListWithCapacity(batchSize);
            fileQueue.drainTo(batch, batchSize);

            analyzeExecutor.execute(() -> {
                checkStateChange();

                AnalystClient analysts = null;
                try {
                    for (; ; ) {
                        analysts = analystService.getAnalystClient();
                        if (analysts.getLoadBalancer().hasHosts()) {
                            // break out of loop when hosts appear.
                            break;
                        } else {
                            try {
                                /**
                                 * No idle analysts were available, waiting.
                                 */
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                return;
                            }
                        }
                    }
                } catch (ArchivistException e) {
                    eventLogService.log(ingest, "Failed to obtain secure SSL connection to analysts, check SSL cert,", e);
                    shutdown();
                }

                if (analysts == null) {
                    logger.warn("Unable to initialize analyst client");
                    return;
                }

                try {
                    analyze(analysts, new AnalyzeRequest()
                            .setUser(user.getUsername())
                            .setIngestId(ingest.getId())
                            .setIngestPipelineId(ingest.getPipelineId())
                            .setAssets(batch)
                            .setProcessors(pipeline.getProcessors()));

                } catch(AbortCrawlerException abort) {
                    // logged elsewhere
                    return;
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

            /**
             * The consumer may call this from multiple threads
             */
            Consumer<AnalyzeRequestEntry> consumer = entry -> {
                checkStateChange();

                totalAssets.increment();
                fileQueue.add(entry);

                if (fileQueue.size() >= batchSize) {
                    submitBatch(pipeline);
                }
            };

            /**
             * For now this is a hard coded list, but we'll need to support plugins
             * for cralwers as well.
             */
            Map<String, Crawler> crawlers = ImmutableMap.of(
                    "file", new FileCrawler(),
                    "http", new HttpCrawler(),
                    "flickr", new FlickrCrawler());

            for (String u : ingest.getUris()) {
                u = u.replaceAll(" ", "%20");
                URI uri = URI.create(u);
                String type = uri.getScheme();
                if (type == null) {
                    type = "file";
                    uri = URI.create("file:" + u);
                }

                logger.info("URI: {}", uri);

                Crawler crawler = crawlers.get(type);
                if (crawler == null) {
                    eventLogService.log("No crawler class for type: '{}'", type);
                    continue;
                }

                crawler.setTargetFileFormats(supportedFormats);
                crawler.start(uri, consumer);
            }

            // The final batch
            submitBatch(pipeline);
        }
    }
}
