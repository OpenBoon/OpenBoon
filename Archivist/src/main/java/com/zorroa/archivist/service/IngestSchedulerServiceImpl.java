package com.zorroa.archivist.service;

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
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

        ingestExecutor.execute(() -> {

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

                ExecutorService executor = Executors.newFixedThreadPool(assetWorkerCount);

                Files.walk(new File(ingest.getPath()).toPath(), FileVisitOption.FOLLOW_LINKS)
                        .filter(p -> p.toFile().isFile())
                        .filter(p -> ingest.isSupportedFileType(FileUtils.extension(p)))
                        .filter(p -> !assetService.assetExistsByPath(p.toFile().toString()))
                        .forEach(t -> {
                            logger.debug("found: {}", t);
                            AssetWorker assetWorker = new AssetWorker(pipeline, ingest, t);
                            if (ArchivistConfiguration.unittest) {
                                assetWorker.run();
                            } else {
                                executor.execute(new AssetWorker(pipeline, ingest, t));
                            }
                        });

                executor.shutdown();
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (Exception e) {
                logger.warn("Failed to execute ingest," + e, e);
            } finally {
                ingestService.setIngestIdle(ingest);
            }
        });
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
            assetService.fastCreateAsset(asset);
        }

        public void executeProcessors() {
            for (IngestProcessorFactory factory: pipeline.getProcessors()) {
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
