package com.zorroa.analyst.service;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.common.service.EventLogService;
import com.zorroa.sdk.client.archivist.ArchivistClient;
import com.zorroa.sdk.config.ApplicationProperties;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.exception.AnalyzeException;
import com.zorroa.sdk.exception.IngestException;
import com.zorroa.sdk.exception.SkipIngestException;
import com.zorroa.sdk.exception.UnrecoverableIngestProcessorException;
import com.zorroa.sdk.filesystem.ObjectFile;
import com.zorroa.sdk.filesystem.ObjectFileSystem;
import com.zorroa.sdk.processor.IngestProcessor;
import com.zorroa.sdk.processor.ProcessorFactory;
import com.zorroa.sdk.schema.ImportSchema;
import com.zorroa.sdk.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by chambers on 2/8/16.
 */
@Component
public class AnalyzeServiceImpl implements AnalyzeService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzeServiceImpl.class);

    @Autowired
    PluginService pluginService;

    @Autowired
    AssetDao assetDao;

    @Autowired
    EventLogService eventLogService;

    @Autowired
    ObjectFileSystem objectFileSystem;

    @Autowired
    TransferService transferService;

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    ListeningExecutorService analyzeExecutor;

    @Autowired
    ArchivistClient archivistClient;

    @Autowired
    RegisterService registerService;
    /**
     * Handles evicting old data from the IngestPipelineCache.
     */
    private final Timer cacheEvictionTimer = new Timer();

    @PostConstruct
    public void init() {
        cacheEvictionTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                pipelineCache.cleanUp();
            }
        }, 60 * 1000, 60 * 1000);
    }

    @Override
    public void asyncAnalyze(AnalyzeRequest req) {
        ListenableFuture<AnalyzeResult> result =
                analyzeExecutor.submit(() -> inlineAnalyze(req));

        Futures.addCallback(result, new FutureCallback<AnalyzeResult>() {
            public void onSuccess(AnalyzeResult result) {
                archivistClient.asyncAnalyzeBatchComplete(req, result);
            }
            public void onFailure(Throwable thrown) {
                AnalyzeResult result = new AnalyzeResult();
                result.addToLogs(thrown.getMessage());
                result.created = 0;
                result.errors = req.getAssetCount();
                result.updated = 0;
                result.retries = 0;
                archivistClient.asyncAnalyzeBatchComplete(req, result);
            }
        });

        /**
         * Update our runtime stats.
         */
        registerService.register();
    }

    @Override
    public AnalyzeResult analyze(AnalyzeRequest req)  {
        try {
            return analyzeExecutor.submit(() -> inlineAnalyze(req)).get();
        } catch (Exception e) {
            throw new AnalyzeException(e.getCause());
        }
    }

    @Override
    public AnalyzeResult inlineAnalyze(AnalyzeRequest req) {

        AnalyzeResult result = new AnalyzeResult();
        List<AssetBuilder> assets = Lists.newArrayListWithCapacity(req.getAssetCount());

        IngestPipelineCacheValue pipeline;
        try {
            pipeline = getProcessingPipeline(req);
        } catch (Exception e) {
            logger.warn("Failed to initialize pipeline, ", e);
            throw new IngestException("Failed to initialize ingest pipeline, " + e.getMessage(), e);
        }

        Queue<AnalyzeRequestEntry> queue = new LinkedBlockingQueue<>();
        queue.addAll(req.getAssets());

        while (!queue.isEmpty()) {
            AnalyzeRequestEntry entry = queue.poll();
            logger.info("analyzing : {}", entry.getUri());

            URI uri = FileUtils.toUri(entry.getUri());
            if (!pipeline.isSupportedFormat(uri.getPath())) {
                logger.debug("The path {} is not a supported format for this pipeline: {}", uri,
                        pipeline.getSupportedFormats());
                continue;
            }

            /**
             * If its a remote entry, we check to see if it exists locally.  If not, use the
             * transferService to download it.
             */

            result.tried++;
            File file;
            ObjectFile obj = null;
            try {
                if (entry.isRemote()) {
                    obj = objectFileSystem.prepare("assets", entry.getUri(), FileUtils.extension(uri.getPath()));
                    if (!obj.exists()) {
                        transferService.transfer(entry.getUri(), obj);
                    }
                    file = obj.getFile();
                }
                else {
                    file = new File(uri);
                }
            } catch (Exception e) {
                eventLogService.log(req, "Ingest error '{}', could not transfer '{}'", e, e.getMessage(), uri);
                result.errors++;
                if (req.getIngestId() == null) {
                    result.addToLogs("Ingest error '{}', could not transfer '{}'", e, e.getMessage(), uri);
                }
                continue;
            }

            if (!file.exists()) {
                eventLogService.log(req, "Ingest error, file does not exist '{}'", file);
                result.errors++;
                if (req.getIngestId() == null) {
                    result.addToLogs("Ingest error, file does not exist '{}'", file);
                }
                continue;
            }

            AssetBuilder builder = new AssetBuilder(file);
            if (obj != null) {
                builder.getSource().setRemoteSourceUri(entry.getUri());
                builder.getSource().setObjectStorageHost(System.getProperty("server.url"));
            }

            /*
             * Translate the attrs sent from the crawler to AssetBuilder
             */
            if (entry.getAttrs() !=  null) {
                for(Map.Entry<String, Object> attr: entry.getAttrs().entrySet()) {
                    if (attr.getKey().startsWith("@")) {
                        builder.addToAttr(attr.getKey().substring(1), attr.getValue());
                    }
                    else {
                        builder.setAttr(attr.getKey(), attr.getValue());
                    }
                }
            }
            /*
             * Populate the previous version.
             */
            builder.setPreviousVersion(
                    assetDao.getByPath(builder.getAbsolutePath()));

            ImportSchema importSchema = builder.getAttr("imports", ImportSchema.class);
            if (importSchema == null) {
                importSchema = new ImportSchema();
            }
            ImportSchema.IngestProperties ingestProperties = importSchema.addIngest(req);
            builder.setAttr("imports", importSchema);

            try {

                /*
                 * Run the ingest processors
                 */
                boolean skip = false;
                for (IngestProcessor processor : pipeline.getProcessors()) {
                    try {
                        if (!processor.isSupportedFormat(builder.getExtension())) {
                            continue;
                        }
                        processor.process(builder);
                        ingestProperties.addToIngestProcessors(processor.getClass().getName());
                    } catch (SkipIngestException skipped) {
                        logger.warn("{} Skipping: {}",  processor.getClass().getName(), skipped.getMessage());
                        skip = true;
                        break;

                    } catch (UnrecoverableIngestProcessorException e) {
                        /*
                         * This exception short circuits the processor. This is handle in outside
                         * catch block.  (see below)
                         */
                        throw e;
                    } catch (Exception e) {
                        eventLogService.log(req, "Ingest warning '{}', processor '{}' failed: '{}'",
                                e, e.getMessage(), processor.getClass().getName(), builder.getAbsolutePath());
                        result.warnings++;
                        if (req.getIngestId() == null) {
                            result.addToLogs("Ingest warning '{}', processor '{}' failed: '{}'",
                                    e, e.getMessage(), processor.getClass().getName(), builder.getAbsolutePath());
                        }
                    }
                }

                if (!skip) {
                    assets.add(builder);

                    Set<String> derived = builder.getLinks().getDerived();
                    if (derived != null) {
                        for (String derivedPath: derived) {
                            AnalyzeRequestEntry childEntry = new AnalyzeRequestEntry(derivedPath);
                            childEntry.addToAttr("links.parents", builder.getId().toString());
                            queue.add(childEntry);
                        }

                        // clear it out.
                        builder.getLinks().setDerived(null);
                    }
                }
            } catch (UnrecoverableIngestProcessorException e) {
                eventLogService.log(req, "Unrecoverable ingest error '{}', processing pipeline failed: '{}'",
                        e, e.getMessage(), builder.getAbsolutePath());
                if (req.getIngestId() == null) {
                    result.addToLogs("Unrecoverable ingest error '{}', processing pipeline failed: '{}'",
                            e, e.getMessage(), builder.getAbsolutePath());
                }
                result.errors++;
            }
            finally {
                builder.close();
                try {
                    if (entry.isRemote()) {
                        builder.getFile().delete();
                    }
                } catch (Exception e ) {
                    logger.warn("Failed to delete file {}", builder.getAbsolutePath());
                }
            }
        }

        if (!req.isDryRun()) {
            logger.debug("Bulk upserting {} assets", assets.size());
            result = assetDao.bulkUpsert(assets).add(result);
            for (String log : result.logs) {
                eventLogService.log(req, log);
            }
        }

        if (req.isReturnAssets()) {
            List<Asset> returnAssets = Lists.newArrayList();
            returnAssets.addAll(assets.stream()
                    .map(builder -> new Asset(builder.getId().toString(), builder.getDocument()))
                    .collect(Collectors.toList()));
            result.setAssets(returnAssets);
        }

        return result;
    }

    /**
     * Wraps an AnalyzeRequest so we can use it as a cache key.
     */
    private static class IngestPipelineCacheKey implements EventLoggable {
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
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("req", req)
                    .add("threadId", threadId)
                    .add("ingestId", ingestId)
                    .add("ingestPipelineId", ingestPipelineId)
                    .toString();
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getThreadId(), getIngestId(), getIngestPipelineId());
        }

        @Override
        public Object getLogId() {
            return ingestId;
        }

        @Override
        public String getLogType() {
            return "Ingest";
        }
    }

    private static class IngestPipelineCacheValue {
        private final List<IngestProcessor> processors;
        private final Set<String> supportedFormats;

        public IngestPipelineCacheValue(List<IngestProcessor> processors, Set<String> supportedFormats) {
            this.processors = processors;
            this.supportedFormats = supportedFormats;
        }

        public Set<String> getSupportedFormats() {
            return supportedFormats;
        }

        public List<IngestProcessor> getProcessors() {
            return processors;
        }

        public boolean isSupportedFormat(String path) {
            return supportedFormats.isEmpty() ? true : supportedFormats.contains(FileUtils.extension(path));
        }
    }

    private final LoadingCache<IngestPipelineCacheKey, IngestPipelineCacheValue> pipelineCache = CacheBuilder.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .removalListener((RemovalListener<IngestPipelineCacheKey, IngestPipelineCacheValue>) r -> {
            logger.info("Tearing down pipeline cache: {}", r.getKey());
            IngestPipelineCacheValue pipeline = r.getValue();
            for (IngestProcessor p: pipeline.getProcessors()) {
                try {
                    p.teardown();
                } catch (Exception e) {
                    logger.warn("Failed to run teardown on {}", p.getClass().getName(), e);
                }
            }
        })
        .build(new CacheLoader<IngestPipelineCacheKey, IngestPipelineCacheValue>() {
            public IngestPipelineCacheValue load(IngestPipelineCacheKey key) throws Exception {
                return initializeIngestPipeline(key.getAnalyzeRequest());
            }
        });

    /**
     * Get the processing pipeline for the give AnalyzeRequest.
     *
     * @param req
     * @return
     * @throws Exception
     */
    private IngestPipelineCacheValue getProcessingPipeline(AnalyzeRequest req) throws Exception {
        if (req.getIngestId() == null || req.getIngestPipelineId() == null) {
            return initializeIngestPipeline(req);
        }
        else {
            return pipelineCache.get(new IngestPipelineCacheKey(req));
        }
    }

    public IngestPipelineCacheValue initializeIngestPipeline(AnalyzeRequest req) throws Exception {
        Set<String> supportedFormats = Sets.newHashSet();
        List<IngestProcessor> result = Lists.newArrayListWithCapacity(req.getProcessors().size());
        for (ProcessorFactory<IngestProcessor> factory : req.getProcessors()) {
            IngestProcessor p = pluginService.getIngestProcessor(factory.getKlass());
            p.setArgs(factory.getArgs());
            p.setApplicationProperties(applicationProperties);
            p.setObjectFileSystem(objectFileSystem);
            p.setArguments();
            try {
                p.init();
            } catch (Exception e) {
                eventLogService.log(req, "Failed to initialize pipeline, {} failed, unexpected {}",
                        e, factory.getKlass(), e.getMessage());
                throw new IngestException("Failed to initialize pipeline, " +
                        factory.getKlass() + "failed, unexpected " + e.getMessage(), e);
            }
            result.add(p);
            supportedFormats.addAll(p.supportedFormats());
        }
        return new IngestPipelineCacheValue(result, supportedFormats);
    }
}
