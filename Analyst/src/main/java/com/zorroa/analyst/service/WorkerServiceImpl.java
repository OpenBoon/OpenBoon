package com.zorroa.analyst.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.zorroa.analyst.domain.BulkAssetUpsertResult;
import com.zorroa.analyst.repository.AssetDao;
import com.zorroa.archivist.sdk.domain.AnalyzeRequest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.exception.UnrecoverableIngestProcessorException;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.apache.tika.Tika;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by chambers on 2/8/16.
 */
@Component
public class WorkerServiceImpl extends AbstractScheduledService implements WorkerService {

    private static final Logger logger = LoggerFactory.getLogger(WorkerServiceImpl.class);

    /**
     * A global ingestor cache, cached based on the class and arguments.
     */
    private final LoadingCache<ProcessorFactory<IngestProcessor>, IngestProcessor> ingestProcessorCache =
            CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .initialCapacity(50)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(new CacheLoader<ProcessorFactory<IngestProcessor>, IngestProcessor>() {
                @Override
                public IngestProcessor load(ProcessorFactory<IngestProcessor> factory) throws Exception {
                    IngestProcessor processor =  factory.getInstance();
                    return processor;
                }
            });

    private static final Tika tika = new Tika();

    private final LinkedBlockingQueue<AssetBuilder> queue = new LinkedBlockingQueue<>();

    @Autowired
    AssetDao assetDao;

    @Autowired
    Client client;

    @PostConstruct
    public void init() {
        startAsync();
    }

    @Override
    public void analyze(AnalyzeRequest req) {

        for (String path: req.getPaths()) {
            AssetBuilder builder = new AssetBuilder(path);

            try {
                /*
                 * Set the previous version of the asset.
                 * asset.setPreviousVersion(assetDao.getByPath(asset.getAbsolutePath()));
                 */

                /*
                 * Use Tika to detect the asset type.
                 */
                builder.getSource().setType(tika.detect(builder.getSource().getPath()));


            } catch (Exception e) {
                /*
                eventLogService.log(ingest, "Ingest error '{}', could not determine asset type on '{}'",
                        e, e.getMessage(), asset.getAbsolutePath());
                messagingService.broadcast(new Message(MessageType.INGEST_EXCEPTION, ingest));

                /*
                 * Can't go further, return.
                 */
                throw new RuntimeException(e);
            }

            try {
                /*
                 * Run the ingest processors
                 */
                for (ProcessorFactory<IngestProcessor> factory: req.getProcessors()) {
                    try {
                        IngestProcessor processor = ingestProcessorCache.get(factory);
                        processor.process(builder);
                    } catch (UnrecoverableIngestProcessorException e) {
                        /*
                         * This exception short circuits the processor. This is handled above.
                         */
                        throw e;
                    } catch (ExecutionException e) {
                        throw new RuntimeException("Failed to find processor instance: " + factory, e);
                    }
                }

                queue.add(builder);

            }
            catch (UnrecoverableIngestProcessorException e) {

            }
            finally {

            }
        }
    }

    @Override
    protected void runOneIteration() throws Exception {
        bulkIndex(250);
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(0, 5, TimeUnit.SECONDS);
    }

    private void bulkIndex(int max) {

        List<AssetBuilder> assets = Lists.newArrayListWithCapacity(Math.max(max, 50));

        if (max > 0) {
            queue.drainTo(assets, max);
        } else {
            queue.drainTo(assets);
        }

        if (assets.isEmpty()) {
            return;
        }

        BulkAssetUpsertResult result = assetDao.bulkUpsert(assets);
    }
}
