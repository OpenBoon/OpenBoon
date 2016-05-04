package com.zorroa.archivist.service;

import com.google.common.collect.*;
import com.zorroa.archivist.ArchivistConfiguration;
import com.zorroa.archivist.AssetExecutor;
import com.zorroa.archivist.domain.UnitTestProcessor;
import com.zorroa.archivist.security.BackgroundTaskAuthentication;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.common.service.EventLogService;
import com.zorroa.sdk.client.ClientException;
import com.zorroa.sdk.client.analyst.AnalystClient;
import com.zorroa.sdk.crawlers.*;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.exception.AbortCrawlerException;
import com.zorroa.sdk.processor.Aggregator;
import com.zorroa.sdk.processor.ProcessorFactory;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
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
    public boolean executeIngest(Ingest ingest) {
        return start(ingest, true /* reset counters */);
    }

    @Override
    public boolean resume(Ingest ingest) {
        return start(ingest, false /*don't reset counters*/);
    }

    protected boolean start(Ingest ingest, boolean firstStart) {
        if (!isHealthy()) {
            eventLogService.log(ingest, "Could not start ingest {}, Zorroa cluster healthy.", ingest);
            return false;
        }

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

    private boolean isHealthy() {
        return healthEndPoint.invoke().getStatus() == Status.UP;
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
        }

        public void shutdown() {
            earlyShutdown = true;
            if (assetExecutor != null) {
                assetExecutor.shutdownNow();
                try {
                    while (!assetExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                        Thread.sleep(250);
                    }
                } catch (InterruptedException e) {
                    logger.warn("Asset processing termination interrupted: " + e.getMessage());
                }
            }
        }

        public void startAggregators(IngestPipeline pipeline) {
            ImmutableList.Builder<Aggregator> builder = ImmutableList.builder();
            AutowireCapableBeanFactory autowire = applicationContext.getAutowireCapableBeanFactory();

            for (ProcessorFactory<Aggregator> factory: pipeline.getAggregators()) {
                Aggregator agg = factory.newInstance();
                autowire.autowireBean(agg);
                agg.init(ingest);
                builder.add(agg);
            }
            aggregators = builder.build();

            aggregationTimer = new Timer(true);
            aggregationTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    aggregators.forEach(Aggregator::aggregate);
                }
            }, 10000, 10000);
        }

        @Override
        public void run() {

            if (!ingestService.setIngestRunning(ingest)) {
                logger.warn("Unable to set ingest {} to the running state.", ingest);
                return;
            }

            /*
             * Create the asset processing thread pool.
             */
            assetExecutor = new AssetExecutor(
                    ingest.getAssetWorkerThreads() > 0 ? ingest.getAssetWorkerThreads() : defaultWorkerThreads);

            try {

                SecurityContextHolder.getContext().setAuthentication(
                        authenticationManager.authenticate(new BackgroundTaskAuthentication(user)));
                IngestPipeline pipeline = ingestService.getIngestPipeline(ingest.getPipelineId());

                /*
                 * Start up the aggregators.
                 */
                startAggregators(pipeline);

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

                /*
                 * Do a final aggregation.
                 */
                aggregators.forEach(Aggregator::aggregate);

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
                    for (AnalyzeRequestEntry asset: req.getAssets()) {
                        AssetBuilder a = new AssetBuilder(asset.getUri());
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
                            messagingService.broadcast(new Message(MessageType.INGEST_EXCEPTION,
                                    ingestService.getIngest(ingest.getId())));
                        }

                        /**
                         * TODO: these exceptions from remove the analyst from the load
                         * balancer.  Once all analysts are removed the ingest
                         * should exit early.
                         */
                    } catch (ClientException e) {
                        /**
                         * This catch block is for handling the case where the AnalystClient
                         * cannot find any hosts to contact.  They are either down,  or rejecting
                         * work for some reason.
                         */
                        ingestService.incrementIngestCounters(ingest, 0, 0, 0, req.getAssetCount());
                        eventLogService.log(ingest, "Failed to contact analyst for processing ingest,", e);
                        messagingService.broadcast(new Message(MessageType.INGEST_EXCEPTION,
                                ingestService.getIngest(ingest.getId())));
                    } catch (Exception e) {
                        /**
                         * This catch block is for handling the case where the Analyst fails
                         * to init or execute the pipeline.  This error is already logged
                         * by the analyst.
                         */
                        ingestService.incrementIngestCounters(ingest, 0, 0, 0, req.getAssetCount());
                        messagingService.broadcast(new Message(MessageType.INGEST_EXCEPTION,
                                ingestService.getIngest(ingest.getId())));
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

            BlockingQueue<AnalyzeRequestEntry> queue = Queues.newLinkedBlockingQueue();

            /**
             * The consumer may call this from multiple threads
             */
            Consumer<AnalyzeRequestEntry> consumer = entry -> {
                if (earlyShutdown) {
                    throw new AbortCrawlerException("Early ingest shutdown.");
                }

                totalAssets.increment();
                queue.add(entry);

                synchronized(queue) {
                    if (queue.size() < batchSize) {
                        return;
                    }
                }

                List<AnalyzeRequestEntry> batch = Lists.newArrayListWithCapacity(batchSize);
                queue.drainTo(batch, batchSize);

                /*
                 * This function returns immediately, leaving the crawler to continue
                 * working.
                 */
                analyze(analyst, new AnalyzeRequest()
                        .setUser(user.getUsername())
                        .setIngestId(ingest.getId())
                        .setIngestPipelineId(ingest.getPipelineId())
                        .setAssets(batch)
                        .setProcessors(pipeline.getProcessors()));
            };

            /**
             * For now this is a hard coded list, but we'll need to support plugins
             * for cralwers as well.
             */
            Map<String, AbstractCrawler> crawlers = ImmutableMap.of(
                    "file", new FileCrawler(),
                    "http", new HttpCrawler(),
                    "flickr", new FlickrCrawler(),
                    "shotgun", new ShotgunCrawler());

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

            /**
             * If we get here, then the crawler is done.  However, the queue might still have data in it.
             */

            if (!queue.isEmpty() && !earlyShutdown) {
                List<AnalyzeRequestEntry> batch = Lists.newArrayListWithCapacity(queue.size());
                queue.drainTo(batch);
                analyze(analyst, new AnalyzeRequest()
                        .setUser(user.getUsername())
                        .setIngestId(ingest.getId())
                        .setIngestPipelineId(ingest.getPipelineId())
                        .setAssets(batch)
                        .setProcessors(pipeline.getProcessors()));
            }
            else {
                queue.clear();
            }
        }
    }
}
