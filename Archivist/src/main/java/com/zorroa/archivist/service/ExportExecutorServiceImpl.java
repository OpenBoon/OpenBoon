package com.zorroa.archivist.service;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.zorroa.archivist.FileUtils;
import com.zorroa.archivist.event.EventServerHandler;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.repository.ExportDao;
import com.zorroa.archivist.repository.ExportOutputDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for scheduling and executing pending exports.
 */
@Component
public class ExportExecutorServiceImpl extends AbstractScheduledService implements ExportExecutorService {

    private static final Logger logger = LoggerFactory.getLogger(ExportExecutorServiceImpl.class);

    @Autowired
    ExportDao exportDao;

    @Autowired
    AssetDao assetDao;

    @Autowired
    ExportOutputDao exportOutputDao;

    @Autowired
    SearchService searchService;

    @Autowired
    EventServerHandler eventServerHandler;

    @Autowired
    ExportOptionsService exportOptionsService;

    @Value("${archivist.export.autoStart}")
    public boolean autoStart;

    @PostConstruct
    public void init() {
        startAsync();
    }

    public void execute(Export export) {

        if (!exportDao.setState(export, ExportState.Running, ExportState.Queued)) {
            logger.warn("Unable to set export '{}' state to running.", export);
            return;
        }
        logger.info("executing export: {}", export);
        Map<ExportOutput, ExportProcessor> outputs = Maps.newHashMap();

        eventServerHandler.broadcast(new Message().setType(
                MessageType.EXPORT_START).setPayload(Json.serializeToString(export)));

        try {

            /*
             * Initialize all the processors
             */
            for (ExportOutput output: exportOutputDao.getAll(export)) {

                /*
                 * Every processor gets its own working directory.
                 */
                ExportProcessor processor = output.getFactory().newInstance();
                FileUtils.makedirs(output.getDirName());

                try {
                    processor.init(export, output);
                    outputs.put(output, processor);

                } catch (Exception e) {
                    logger.warn("Failed to initialize output '{}',", output.getFactory().getKlassName(), e);
                }
            }

            if (outputs.isEmpty()) {
                logger.warn("All output processors failed to initialize for export: {}", export);
                return;
            }

            /*
             * Note that, since this is most likely going to be a scan and scroll, the
             * asset loops is on the outside. We don't really want to do it more than once,
             * otherwise one output might have different contents. (couldn't see way to
             * rewind)
             */
            for (Asset asset : searchService.scanAndScroll(export.getSearch())) {
                logger.info("processing asset {}", (String) asset.getValue("source.path"));

                for (Map.Entry<ExportOutput, ExportProcessor> entry : outputs.entrySet()) {
                    ExportProcessor processor = entry.getValue();
                    ExportOutput output = entry.getKey();

                    try {
                        processor.process(exportOptionsService.applyOptions(export, output, asset));
                        assetDao.addToExport(asset, export);

                        eventServerHandler.broadcast(new Message().setType(
                                MessageType.EXPORT_ASSET).setPayload(asset.getId()));

                    } catch (Exception e) {
                        /*
                         * exportOptionsService.applyOptions may throw an exception if there
                         * is an error processing the source data.
                         */
                        logger.warn("Failed to add asset {} to output '{}',", asset, e);
                    }
                }
            }

            for (Map.Entry<ExportOutput, ExportProcessor> entry: outputs.entrySet()) {
                ExportProcessor processor = entry.getValue();
                ExportOutput output = entry.getKey();

                logger.info("tearing down processor {}", processor);
                try {
                    processor.teardown();
                } catch (Exception e) {
                    logger.warn("Failed to tear down processor '{}',", processor, e);
                }

                eventServerHandler.broadcast(new Message().setType(
                        MessageType.EXPORT_OUTPUT_STOP).setPayload(Json.serializeToString(output)));
            }

        } finally {
            if (exportDao.setState(export, ExportState.Finished, ExportState.Running)) {
                eventServerHandler.broadcast(new Message().setType(
                        MessageType.EXPORT_STOP).setPayload(Json.serializeToString(export)));
            }
        }
    }

    @Override
    protected void runOneIteration() throws Exception {

        if (!autoStart) {
            return;
        }

        for (Export export: exportDao.getAll(ExportState.Queued, 10)) {
            /*
             * Catch exceptions here just in case. Exceptions that bubble out of here
             * wills top the scheduler.
             */
            try {
                execute(export);
            } catch (Exception e) {
                logger.warn("Error starting export {}, ", export, e);
            }
        }
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedDelaySchedule(10, 1, TimeUnit.SECONDS);
    }
}
