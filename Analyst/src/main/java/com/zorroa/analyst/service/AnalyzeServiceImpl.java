package com.zorroa.analyst.service;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.domain.AnalyzeRequest;
import com.zorroa.archivist.sdk.domain.AnalyzeResult;
import com.zorroa.archivist.sdk.domain.ApplicationProperties;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.exception.IngestException;
import com.zorroa.archivist.sdk.exception.UnrecoverableIngestProcessorException;
import com.zorroa.archivist.sdk.filesystem.ObjectFileSystem;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.IngestSchema;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.common.service.EventLogService;
import org.apache.tika.Tika;
import org.elasticsearch.indices.IndexMissingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by chambers on 2/8/16.
 */
@Component
public class AnalyzeServiceImpl implements AnalyzeService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzeServiceImpl.class);

    private static final Tika tika = new Tika();

    @Autowired
    AssetDao assetDao;

    @Autowired
    EventLogService eventLogService;

    @Autowired
    ObjectFileSystem objectFileSystem;

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    AsyncTaskExecutor ingestThreadPool;

    @Override
    public AnalyzeResult analyze(AnalyzeRequest req) {
        try {
            return ingestThreadPool.submit(() -> asyncAnalyze(req)).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            RuntimeException re = (RuntimeException) e.getCause();
            throw re;
        }
    }

    private AnalyzeResult asyncAnalyze(AnalyzeRequest req) {

        AnalyzeResult result = new AnalyzeResult();
        List<AssetBuilder> assets = Lists.newArrayListWithCapacity(req.getPaths().size());
        List<IngestProcessor> processors;

        try {
            processors = getProcessingPipeline(req);
        } catch (Exception e) {
            throw new IngestException("Failed to initialize ingest pipeline", e.getCause());
        }

        IngestSchema ingestSchema = null;
        if (req.getIngestId() != null) {
            ingestSchema = new IngestSchema();
            ingestSchema.addIngest(req);
        }

        for (String path: req.getPaths()) {
            logger.info("processing: {}", path);
            result.tried++;
            AssetBuilder builder = new AssetBuilder(path);
            if (ingestSchema != null) {
                builder.addSchema(ingestSchema);
            }

            try {
                builder.getSource().setType(tika.detect(builder.getSource().getPath()));
            } catch (Exception e) {
                eventLogService.log(req, "Ingest error '{}', could not determine asset type on '{}'",
                        e, e.getMessage(), builder.getAbsolutePath());
                result.errors++;
                continue;
            }

            try {
                builder.setPreviousVersion(
                        assetDao.getByPath(builder.getAbsolutePath()));
            } catch (IndexMissingException e) {
                eventLogService.log(req, "Ingest error '{}', could not populate previous asset '{}'",
                        e, e.getMessage(), builder.getAbsolutePath());
            }

            try {

                /*
                 * Run the ingest processors
                 */
                for (IngestProcessor processor : processors) {
                    try {
                        if (!processor.isSupportedFormat(builder.getExtension())) {
                            continue;
                        }
                        processor.process(builder);
                    } catch (UnrecoverableIngestProcessorException e) {
                        /*
                         * This exception short circuits the processor. This is handle in outside
                         * catch block.  (see below)
                         */
                        throw e;
                    } catch (Exception e) {
                        eventLogService.log(req, "Ingest warning '{}', processing pipeline failed: '{}'",
                                e, e.getMessage(), builder.getAbsolutePath());
                        result.warnings++;
                    }
                }

                assets.add(builder);

            } catch (UnrecoverableIngestProcessorException e) {
                eventLogService.log(req, "Unrecoverable ingest error '{}', processing pipeline failed: '{}'",
                        e, e.getMessage(), builder.getAbsolutePath());
                result.errors++;
            }
        }

        result = assetDao.bulkUpsert(assets).add(result);
        if (!result.logs.isEmpty()) {
            for (String log: result.logs) {
                eventLogService.log(req, log);
            }
            result.logs.clear();
        }
        return result;
    }

    /**
     * Wraps an AnalyzeRequest so we can use it as a cache key.
     */
    private static class IngestPipelineCacheKey {
        private final AnalyzeRequest req;
        private final long threadId;
        private final int ingestId;
        private final int ingestPipelineId;

        public IngestPipelineCacheKey(AnalyzeRequest req) {
            Preconditions.checkNotNull(req.getIngestId(), "IngestId cannot be null");
            Preconditions.checkNotNull(req.getIngestPipelineId(), "IngestPipelineId cannot be null");
            Preconditions.checkArgument(req.getProcessors().size() > 0, "The ingest pipeline contains no processors");

            this.req = req;
            this.threadId = Thread.currentThread().getId();
            this.ingestId = req.getIngestId();
            this.ingestPipelineId = req.getIngestPipelineId();
        }

        public int getIngestId() {
            return ingestId;
        }

        public int getIngestPipelineId() {
            return ingestPipelineId;
        }

        public long getThreadId() {
            return threadId;
        }

        public AnalyzeRequest getAnalyzeRequest() {
            return req;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IngestPipelineCacheKey that = (IngestPipelineCacheKey) o;
            return getThreadId() == that.getThreadId() &&
                    getIngestId() == that.getIngestId() &&
                    getIngestPipelineId() == that.getIngestPipelineId();
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getThreadId(), getIngestId(), getIngestPipelineId());
        }
    }

    private final LoadingCache<IngestPipelineCacheKey, List<IngestProcessor>> pipelineCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(new CacheLoader<IngestPipelineCacheKey, List<IngestProcessor>>() {
                public List<IngestProcessor> load(IngestPipelineCacheKey key) throws Exception {
                    List<IngestProcessor> result = Lists.newArrayListWithCapacity(key.getAnalyzeRequest().getProcessors().size());
                    for (ProcessorFactory<IngestProcessor> factory : key.getAnalyzeRequest().getProcessors()) {
                        IngestProcessor p = factory.newInstance();
                        p.setApplicationProperties(applicationProperties);
                        p.setObjectFileSystem(objectFileSystem);
                        p.init();
                        result.add(p);
                    }
                    return result;
                }
            });

    /**
     *
     * Create the processing pipeline.
     *
     * @param req
     * @return
     * @throws Exception
     */
    private List<IngestProcessor> getProcessingPipeline(AnalyzeRequest req) throws ExecutionException {
        return pipelineCache.get(new IngestPipelineCacheKey(req));
    }
}
