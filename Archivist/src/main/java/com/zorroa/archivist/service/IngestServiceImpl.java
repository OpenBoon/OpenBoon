package com.zorroa.archivist.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.elasticsearch.common.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import com.zorroa.archivist.FileUtils;
import com.zorroa.archivist.IngestException;
import com.zorroa.archivist.domain.AssetBuilder;
import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.domain.IngestPipeline;
import com.zorroa.archivist.domain.IngestPipelineBuilder;
import com.zorroa.archivist.domain.IngestProcessorFactory;
import com.zorroa.archivist.domain.ProxyConfig;
import com.zorroa.archivist.domain.ProxyOutput;
import com.zorroa.archivist.processors.IngestProcessor;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.repository.IngestPipelineDao;
import com.zorroa.archivist.repository.ProxyConfigDao;

/**
 *
 * The ingest service is responsible for processing IngestRequest objects which
 * results in the creation of assets using the assetService.
 *
 * @author chambers
 *
 */
@Component
public class IngestServiceImpl implements IngestService, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(IngestServiceImpl.class);

    ApplicationContext applicationContext;

    @Autowired
    IngestPipelineDao ingestPipelineDao;

    @Autowired
    AssetDao assetDao;

    @Autowired
    ProxyConfigDao proxyConfigDao;

    @Autowired
    @Qualifier("ingestTaskExecutor")
    TaskExecutor ingestExecutor;

    @Autowired
    @Qualifier("processorTaskExecutor")
    TaskExecutor processorExecutor;

    @Override
    public IngestPipeline createIngestPipeline(IngestPipelineBuilder builder) {
        return ingestPipelineDao.create(builder);
    }

    @Override
    public IngestPipeline getIngestPipeline(String id) {
        return ingestPipelineDao.get(id);
    }

    @Override
    public List<IngestPipeline> getIngestPipelines() {
        return ingestPipelineDao.getAll();
    }

    @Override
    public void ingest(IngestPipeline pipeline, IngestBuilder builder) {

        ProxyConfig proxyConfig = proxyConfigDao.get(builder.getProxyConfig());
        Preconditions.checkNotNull(proxyConfig, "Could not find ProxyConfig: " + builder.getProxyConfig());
        List<ProxyOutput> proxyOutputs = proxyConfigDao.getAllOutputs(proxyConfig);

        List<IngestProcessorFactory> processors = pipeline.getProcessors();

        /**
         * Initialize all the processors
         */
        for (IngestProcessorFactory factory: processors) {
            IngestProcessor processor = factory.init();
            if (processor == null ) {
                String msg = "Aborting ingest, processor not found:" + factory.getKlass();
                logger.warn(msg);
                // TODO: set the ingest state to failed.
                throw new IngestException(msg);
            }
            Preconditions.checkNotNull(processor, "The IngestProcessor class: " + factory.getKlass() +
                    " was not found, aborting ingest");

            AutowireCapableBeanFactory autowire = applicationContext.getAutowireCapableBeanFactory();
            autowire.autowireBean(processor);
            processor.setProxyOutputs(proxyOutputs);
        }

        ingestExecutor.execute(new IngestWorker(pipeline, builder));
    }

    /**
     * Each ingest gets one IngestWorker.
     *
     * @author chambers
     *
     */
    private class IngestWorker implements Runnable {

        private final IngestPipeline pipeline;
        private final IngestBuilder builder;

        public IngestWorker(IngestPipeline pipeline, IngestBuilder builder) {
            this.pipeline = pipeline;
            this.builder = builder;
        }

        @Override
        public void run() {

            logger.info("Starting ingest worker pipeline={},  {} -> {}",
                    new Object[] { pipeline.getId(), builder.getPath(), builder.getFileTypes() });

            try {
                Files.walk(new File(builder.getPath()).toPath(), FileVisitOption.FOLLOW_LINKS)
                    .filter(p -> p.toFile().isFile())
                    .filter(p -> builder.isSupportedFileType(FileUtils.extension(p)))
                    .forEach(new Consumer<Path>() {
                        @Override
                        public void accept(Path t) {
                            logger.info("found: {}", t);
                            processorExecutor.execute(new AssetWorker(pipeline, builder, t));
                        }
                    });
            } catch (IOException e) {
                logger.warn("Ingest worker failed:", e);
            }

            logger.info("Stopping ingest worker pipeline={},  {} -> {}",
                    new Object[] { pipeline.getId(), builder.getPath(), builder.getFileTypes() });
        }
    }

    private class AssetWorker implements Runnable {

        private final IngestPipeline pipeline;
        private final IngestBuilder builder;
        private final AssetBuilder asset;

        public AssetWorker(IngestPipeline pipeline, IngestBuilder builder, Path path) {
            this.pipeline = pipeline;
            this.builder = builder;
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
            asset.put("ingest", "builder", builder.getPath());
            asset.put("ingest", "time", System.currentTimeMillis());

            /*
             * Execute all the processor which are part of the pipeline.
             */
            executeProcessors();

            /*
             * Finally, create the asset.
             */
            logger.debug("Creating asset: {}", asset);
            assetDao.create(asset);
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
    }
}

