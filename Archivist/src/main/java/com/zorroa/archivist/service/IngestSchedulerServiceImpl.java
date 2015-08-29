package com.zorroa.archivist.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zorroa.archivist.ArchivistConfiguration;
import com.zorroa.archivist.AssetExecutor;
import com.zorroa.archivist.FileUtils;
import com.zorroa.archivist.IngestException;
import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestPipeline;
import com.zorroa.archivist.domain.IngestProcessorFactory;
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
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

@Component
public class IngestSchedulerServiceImpl implements IngestSchedulerService {

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

    private final ConcurrentMap<Long, IngestWorker> runningIngests = Maps.newConcurrentMap();

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
        IngestWorker worker = new IngestWorker(ingest);

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

        /**
         * Counters for creates, updates, and errors.
         */
        private LongAdder createdCount = new LongAdder();
        private LongAdder updatedCount = new LongAdder();
        private LongAdder errorCount = new LongAdder();

        /**
         * A timer thread for updating counts.
         */
        private Timer updateCountsTimer;

        private AssetExecutor assetExecutor;

        private final Ingest ingest;

        private boolean earlyShutdown = false;

        public IngestWorker(Ingest ingest) {
            this.ingest = ingest;
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

            // Keep a list of the processor instances which we'll use for
            // running the tear down later on.
            List<IngestProcessor> processors = Lists.newArrayList();

            try {
                /*
                 * Initialize everything we need to run this ingest
                 */
                IngestPipeline pipeline = ingestService.getIngestPipeline(ingest.getPipelineId());

                for (IngestProcessorFactory factory : pipeline.getProcessors()) {
                    IngestProcessor processor = factory.init();
                    if (processor == null) {
                        String msg = "Aborting ingest, processor not found:" + factory.getKlass();
                        logger.warn(msg);
                        throw new IngestException(msg);
                    }
                    Preconditions.checkNotNull(processor, "The IngestProcessor class: " + factory.getKlass() +
                            " was not found, aborting ingest");

                    AutowireCapableBeanFactory autowire = applicationContext.getAutowireCapableBeanFactory();
                    autowire.autowireBean(processor);
                    processor.setIngestProcessorService(ingestProcessorService);
                    processors.add(processor);
                }

                updateCountsTimer = new Timer();
                updateCountsTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        ingestService.updateIngestCounters(ingest,
                                createdCount.intValue(),
                                updatedCount.intValue(),
                                errorCount.intValue());
                    }
                }, 1000, 1000);

                Path start = new File(ingest.getPath()).toPath();
                Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                            throws IOException {
                        if (!file.toFile().isFile()) {
                            return FileVisitResult.CONTINUE;
                        }

                        if (!ingest.isSupportedFileType(FileUtils.extension(file))) {
                            return FileVisitResult.CONTINUE;
                        }

                        if (logger.isDebugEnabled()) {
                            logger.debug("Found file: {}", file);
                        }

                        AssetWorker assetWorker = new AssetWorker(pipeline, ingest, file);
                        if (ArchivistConfiguration.unittest) {
                            assetWorker.run();
                        } else {
                            assetExecutor.execute(assetWorker);
                        }

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException e)
                            throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                });

                /*
                 * Block forever until the queue is empty and all threads
                 * have stopped working.
                 */
                assetExecutor.waitForCompletion();
                logger.info("Ingest {} finished successfully.", ingest);

            } catch (Exception e) {
                logger.warn("Failed to execute ingest," + e, e);
            } finally {
                updateCountsTimer.cancel();
                runningIngests.remove(ingest.getId());
                processors.forEach(p->p.teardown());
                if (!earlyShutdown) {       // Avoid if paused or interrupted
                    ingestService.updateIngestStopTime(ingest, System.currentTimeMillis());
                    ingestService.updateIngestCounters(ingest,
                            createdCount.intValue(),
                            updatedCount.intValue(),
                            errorCount.intValue());
                    ingestService.setIngestIdle(ingest);
                }
            }
        }

        private class AssetWorker implements Runnable {

            private final IngestPipeline pipeline;
            private final Ingest ingest;
            private final AssetBuilder asset;

            public AssetWorker(IngestPipeline pipeline, Ingest ingest, Path path) {
                this.pipeline = pipeline;
                this.ingest = ingest;
                this.asset = new AssetBuilder(path.toFile());
                this.asset.setAsync(true);
            }

            @Override
            public void run() {

                //
                // Put a try block here so the thread doesn't exit after an exception.
                //
                try {
                    // Skip assets that were index after the start of the current ingest.
                    // FIXME: This fails when two ingests overlap in time and share files.
                    //        Fixable with per-ingest start times using ingest list below.
                    if (assetService.assetExistsByPathAfter(asset.getAbsolutePath().toString(),
                            ingest.getTimeStarted())) {
                        return;
                    }

                    if (!ingest.isUpdateOnExist()) {
                        if (assetService.assetExistsByPath(asset.getAbsolutePath().toString())) {
                            return;
                        }
                    }

                    logger.debug("Ingesting: {}", asset);

                    // Store per-ingest id and time info.
                    // Store last time for each ingest to properly handle overlap.
                    asset.put("ingest", "pipeline", pipeline.getId());
                    asset.put("ingest", "builder", ingest.getPath());
                    asset.put("ingest", "time", System.currentTimeMillis());

                    // Run the ingest processors to augment the AssetBuilder
                    executeProcessors();

                    // Store the asset using the final builder
                    logger.debug("Creating asset: {}", asset);
                    if (assetService.replaceAsset(asset)) {
                        updatedCount.increment();
                    } else {
                        createdCount.increment();
                    }
                }
                catch (Exception e) {
                    logger.warn("Failed to execute ingest for asset '{}',", asset.getAbsolutePath(), e);
                }
            }

            public void executeProcessors() {
                for (IngestProcessorFactory factory : pipeline.getProcessors()) {
                    try {
                        IngestProcessor processor = factory.getProcessor();
                        logger.debug("running processor: {}", processor.getClass());
                        processor.process(asset);
                    } catch (Exception e) {
                        errorCount.increment();
                        logger.warn("Processor {} failed to run on asset {}",
                                factory.getProcessor().getClass().getCanonicalName(), asset.getFile(), e);
                    }
                }
            }
        }
    }
}
