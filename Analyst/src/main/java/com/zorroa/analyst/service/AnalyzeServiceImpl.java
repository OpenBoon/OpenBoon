package com.zorroa.analyst.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.zorroa.analyst.repository.AssetDao;
import com.zorroa.archivist.sdk.domain.AnalyzeRequest;
import com.zorroa.archivist.sdk.domain.AnalyzeResult;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.exception.UnrecoverableIngestProcessorException;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.common.service.EventLogService;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * A global ingestor cache, cached based on the class and arguments.
     */
    private final LoadingCache<ProcessorFactory<IngestProcessor>, IngestProcessor> ingestProcessorCache =
            CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .initialCapacity(50)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(new CacheLoader<ProcessorFactory<IngestProcessor>, IngestProcessor>() {
                @Override
                public IngestProcessor load(ProcessorFactory<IngestProcessor> factory) throws Exception {
                    IngestProcessor processor =  factory.newInstance();
                    processor.init();
                    return processor;
                }
            });

    @Autowired
    AssetDao assetDao;

    @Autowired
    EventLogService eventLogService;

    @Override
    public AnalyzeResult analyze(AnalyzeRequest req) {

        List<AssetBuilder> result = Lists.newArrayListWithCapacity(req.getPaths().size());

        for (String path: req.getPaths()) {
            AssetBuilder builder = new AssetBuilder(path);

            try {
                /*
                 * Set the previous version of the asset.
                 * asset.setPreviousVersion(assetDao.getByPath(asset.getAbsolutePath()));
                 */

                builder.getSource().setType(tika.detect(builder.getSource().getPath()));


            } catch (Exception e) {
                eventLogService.log(req, "Ingest error '{}', could not determine asset type on '{}'",
                        e, e.getMessage(), builder.getAbsolutePath());

                /*
                 * Can't go further, return.
                 */
                throw new RuntimeException(e);
            }

            try {
                /*
                 * Run the ingest processors
                 */
                for (ProcessorFactory<IngestProcessor> factory : req.getProcessors()) {
                    try {
                        IngestProcessor processor = ingestProcessorCache.get(factory);
                        if (!processor.isSupportedFormat(builder.getExtension())) {
                            continue;
                        }
                        processor.process(builder);
                    } catch (ExecutionException | UnrecoverableIngestProcessorException e) {
                        /*
                         * This exception short circuits the processor. This is handle in outside
                         * catch block.  (see below)
                         */
                        throw e;
                    } catch (Exception e) {
                        eventLogService.log(req, "Ingest warning '{}', processing pipeline failed: '{}'",
                                e, e.getMessage(), builder.getAbsolutePath());
                    }
                }

                result.add(builder);

            } catch (ExecutionException | UnrecoverableIngestProcessorException e) {
                eventLogService.log(req, "Unrecoverable ingest error '{}', processing pipeline failed: '{}'",
                        e, e.getMessage(), builder.getAbsolutePath());
            }
        }

        return assetDao.bulkUpsert(result);
    }
}
