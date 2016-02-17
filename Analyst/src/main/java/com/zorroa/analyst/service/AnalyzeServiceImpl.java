package com.zorroa.analyst.service;

import com.google.common.collect.Lists;
import com.zorroa.analyst.repository.AssetDao;
import com.zorroa.archivist.sdk.domain.AnalyzeRequest;
import com.zorroa.archivist.sdk.domain.AnalyzeResult;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.exception.IngestException;
import com.zorroa.archivist.sdk.exception.UnrecoverableIngestProcessorException;
import com.zorroa.archivist.sdk.filesystem.ObjectFileSystem;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.IngestSchema;
import com.zorroa.common.service.EventLogService;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

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

    @Override
    public AnalyzeResult analyze(AnalyzeRequest req) {

        List<AssetBuilder> result = Lists.newArrayListWithCapacity(req.getPaths().size());
        List<IngestProcessor> processors;

        try {
            processors = createProcessingPipeline(req);
        } catch (Exception e) {
            throw new IngestException("Failed to initialize ingest pipeline, " + e.getMessage(), e);
        }

        IngestSchema ingestSchema = new IngestSchema();
        ingestSchema.addIngest(req);

        for (String path: req.getPaths()) {
            logger.info("processing: {}", path);
            AssetBuilder builder = new AssetBuilder(path);
            builder.addSchema(ingestSchema);

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
                    }
                }

                result.add(builder);

            } catch (UnrecoverableIngestProcessorException e) {
                eventLogService.log(req, "Unrecoverable ingest error '{}', processing pipeline failed: '{}'",
                        e, e.getMessage(), builder.getAbsolutePath());
            }
        }

        return assetDao.bulkUpsert(result);
    }

    /**
     *
     * Create the processing pipeline.
     * TODO: add caching to this.
     *
     * @param req
     * @return
     * @throws Exception
     */
    private List<IngestProcessor> createProcessingPipeline(AnalyzeRequest req) throws Exception {

        List<IngestProcessor> result = Lists.newArrayListWithCapacity(req.getProcessors().size());
        for (ProcessorFactory<IngestProcessor> factory : req.getProcessors()) {
            IngestProcessor p = factory.newInstance();
            p.setObjectFileSystem(objectFileSystem);
            p.init();
            result.add(p);
        }

        return result;
    }
}
