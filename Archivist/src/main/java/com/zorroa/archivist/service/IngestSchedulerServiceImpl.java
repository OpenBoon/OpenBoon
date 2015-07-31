package com.zorroa.archivist.service;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.*;
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
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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

    private Executor assetExecutor;

    @PostConstruct
    public void init() {
        ingestExecutor = Executors.newFixedThreadPool(ingestWorkerCount);
        assetExecutor = Executors.newFixedThreadPool(assetWorkerCount);
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
        executeIngest(ingest, false);
    }

    @Override
    public void executeIngest(Ingest ingest, boolean paused) {
        IngestWorker worker = new IngestWorker(ingest);
        worker.setPaused(paused);

        if (runningIngests.putIfAbsent(ingest.getId(), worker) == null) {

            logger.info("adding ingest working to map: {}", worker);

            if (ArchivistConfiguration.unittest) {
                worker.run();
            }
            else {
                ingestExecutor.execute(worker);
            }
        } else {
            logger.warn("The ingest is already executing: {}", ingest);
        }
    }

    @Override
    public boolean pause(Ingest ingest) {
        IngestWorker worker = runningIngests.get(ingest.getId());
        if (worker == null) {
            return false;
        }
        if (worker.setPaused(true)) {
            ingestService.setIngestPaused(ingest, true);
            return true;
        }
        return false;
    }

    @Override
    public boolean resume(Ingest ingest) {
        IngestWorker worker = runningIngests.get(ingest.getId());
        if (worker == null) {
            return false;
        }

        if (worker.setPaused(false)) {
            logger.info("ingest unpaused");
            ingestService.setIngestPaused(ingest, false);
            logger.info("notifying ingest worker {}", worker);
            logger.info("done");
            return true;
        }

        return false;
    }

    public class IngestWorker implements Runnable {

        /**
         * An Object to synchronized around for wait/notify.
         */
        private Object pausedMutex = new Object();

        /**
         * An AtomicBoolean for setting the paused state.
         */
        private AtomicBoolean paused = new AtomicBoolean(false);

        /**
         * A count down latch for determining if the ingest is complete.
         */
        private AtomicLong latch = new AtomicLong(0);

        private final Ingest ingest;

        public IngestWorker(Ingest ingest) {
            this.ingest = ingest;
        }

        public boolean setPaused(boolean value) {
            boolean result = paused.compareAndSet(!value, value);
            synchronized (pausedMutex) {
                if (!value) {
                    pausedMutex.notify();
                }
            }

            return result;
        }

        @Override
        public void run() {

            if (!ingestService.setIngestRunning(ingest)) {
                logger.warn("Unable to set ingest {} to the running state.", ingest);
                return;
            }

            try {
                /*
                 * Initialize everything we need to run this ingest
                 */
                IngestPipeline pipeline = ingestService.getIngestPipeline(ingest.getPipelineId());
                List<IngestProcessorFactory> processors = pipeline.getProcessors();

                for (IngestProcessorFactory factory : processors) {
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
                }

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

                        if (paused.get() == true) {
                            logger.info("Ingest thread paused {} {}", this, ingest);
                            try {
                                synchronized(pausedMutex) {
                                    pausedMutex.wait();
                                }
                                logger.info("Ingest thread resumed {}", ingest);
                            } catch (InterruptedException ignore) {
                                // if the thread is interrupted then just terminate;
                                return FileVisitResult.TERMINATE;
                            }
                        }

                        /*
                         * Increment a number for every worker we create.
                         * The worker will decrement it when done.
                         */
                        latch.incrementAndGet();

                        AssetWorker assetWorker = new AssetWorker(pipeline, ingest, file, latch);
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

                while (latch.get() != 0) {
                    Thread.sleep(1000);
                }

                logger.info("Ingest {} finished", ingest);

            } catch (Exception e) {
                logger.warn("Failed to execute ingest," + e, e);
            } finally {
                runningIngests.remove(ingest.getId());
                ingestService.setIngestIdle(ingest);
            }
        }

        private class AssetWorker implements Runnable {

            private final IngestPipeline pipeline;
            private final Ingest ingest;
            private final AssetBuilder asset;
            private final AtomicLong latch;

            public AssetWorker(IngestPipeline pipeline, Ingest ingest, Path path, AtomicLong latch) {
                this.pipeline = pipeline;
                this.ingest = ingest;
                this.latch = latch;
                this.asset = new AssetBuilder(path.toFile());
                this.asset.setAsync(true);
            }

            @Override
            public void run() {

                try {
                    if (!ingest.isUpdateOnExist()) {
                        if (assetService.assetExistsByPath(asset.getAbsolutePath().toString())) {
                            return;
                        }
                    }

                    logger.debug("Ingesting: {}", asset);
                    /*
                     * Add some standard keys to the document
                     */
                    asset.put("ingest", "pipeline", pipeline.getId());
                    asset.put("ingest", "builder", ingest.getPath());
                    asset.put("ingest", "time", System.currentTimeMillis());

                    /*
                     * Execute all the processor which are part of the pipeline.
                     */
                    executeProcessors();

                    /*
                     * Finally, create the asset.
                     */
                    logger.debug("Creating asset: {}", asset);
                    assetService.replaceAsset(asset);
                } finally {
                    latch.decrementAndGet();
                }
            }

            public void executeProcessors() {
                for (IngestProcessorFactory factory : pipeline.getProcessors()) {
                    try {
                        IngestProcessor processor = factory.getProcessor();
                        logger.debug("running processor: {}", processor.getClass());
                        processor.process(asset);
                    } catch (Exception e) {
                        logger.warn("Processor {} failed to run on asset {}",
                                factory.getProcessor().getClass().getCanonicalName(), asset.getFile(), e);
                    }
                }
            }
        }
    }
}
