package com.zorroa.archivist.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import com.zorroa.archivist.FileUtils;
import com.zorroa.archivist.domain.AssetBuilder;
import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.domain.IngestPipeline;
import com.zorroa.archivist.domain.IngestPipelineBuilder;
import com.zorroa.archivist.domain.IngestProcessorFactory;
import com.zorroa.archivist.ingest.IngestProcessor;
import com.zorroa.archivist.repository.AssetDaoImpl;
import com.zorroa.archivist.repository.IngestPipelineDao;

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
    AssetDaoImpl assetDao;

    @Autowired
    @Qualifier("ingestTaskExecutor")
    AsyncTaskExecutor ingestExecutor;

    @Autowired
    @Qualifier("processorTaskExecutor")
    AsyncTaskExecutor processorExecutor;

    @Override
    public IngestPipeline createIngestPipeline(IngestPipelineBuilder builder) {
        String id = ingestPipelineDao.create(builder);
        return ingestPipelineDao.get(id);
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
        /**
         * Initialize all the processors
         */
        for (IngestProcessorFactory factory: pipeline.getProcessors()) {
            IngestProcessor processor = factory.init();
            AutowireCapableBeanFactory autowire = applicationContext.getAutowireCapableBeanFactory();
            autowire.autowireBean(processor);
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
                Files.list(new File(builder.getPath()).toPath())
                    .filter(p -> builder.getFileTypes().contains(FileUtils.extension(p.getFileName())))
                    .forEach(new Consumer<Path>() {
                        @Override
                        public void accept(Path t) {
                            logger.info("found: {}", t);
                            processorExecutor.execute(new AssetWorker(pipeline, builder, t));
                        }

                    });
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    private class AssetWorker implements Runnable {

        private final IngestPipeline pipeline;
        private final IngestBuilder builder;
        private final Path path;

        public AssetWorker(IngestPipeline pipeline, IngestBuilder builder, Path path) {
            this.pipeline = pipeline;
            this.builder = builder;
            this.path = path;
        }

        @Override
        public void run() {

            logger.info("Ingesting: {}", path);

            File file = path.toFile();
            final AssetBuilder asset = new AssetBuilder();
            asset.setAsync(true);

            asset.put("ingest", "pipeline", pipeline.getId());
            asset.put("ingest", "time", System.currentTimeMillis());

            asset.put("source", "filename", path.getFileName().toString());
            asset.put("source", "directory", path.getParent().toString());

            for (IngestProcessorFactory factory: pipeline.getProcessors()) {
                factory.getProcessor().process(asset, file);
            }

            assetDao.create(asset);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
    }
}

