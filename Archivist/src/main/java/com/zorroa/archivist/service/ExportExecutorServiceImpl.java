package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.zorroa.archivist.processors.ExportDateAggregator;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.repository.ExportDao;
import com.zorroa.archivist.repository.ExportOutputDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.service.ExportService;
import com.zorroa.archivist.sdk.service.MessagingService;
import com.zorroa.archivist.sdk.service.UserService;
import com.zorroa.archivist.sdk.util.FileUtils;
import com.zorroa.archivist.sdk.util.Json;
import com.zorroa.archivist.sdk.util.StringUtil;
import com.zorroa.archivist.security.BackgroundTaskAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for scheduling and executing pending exports.
 */
@Component
public class ExportExecutorServiceImpl extends AbstractScheduledService implements ExportExecutorService {

    private static final Logger logger = LoggerFactory.getLogger(ExportExecutorServiceImpl.class);

    @Autowired
    ExportService exportService;

    @Autowired
    ExportDao exportDao;

    @Autowired
    AssetDao assetDao;

    @Autowired
    ExportOutputDao exportOutputDao;

    @Autowired
    SearchService searchService;

    @Autowired
    UserService userService;

    @Autowired
    MessagingService messagingService;

    @Autowired
    ExportOptionsService exportOptionsService;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    ApplicationContext applicationContext;

    @Value("${archivist.export.autoStart}")
    public boolean autoStart;

    @Value("${archivist.export.expireTime}")
    private String expireTime;

    @PostConstruct
    public void init() {
        startAsync();
    }

    public void execute(Export export) {

        Map<ExportOutput, ExportProcessor> outputs = Maps.newHashMap();
        int assetCount = 0;

        /*
        * Set the authentication to the user that created the export.
        */
        User user = userService.get(export.getUserCreated());
        SecurityContextHolder.getContext().setAuthentication(
                authenticationManager.authenticate(new BackgroundTaskAuthentication(user)));

        if (!exportDao.setRunning(export)) {
            logger.warn("Unable to set export '{}' state to running.  In not in queued state.", exportDao.get(export.getId()));
            return;
        }
        logger.info("executing export: {}", export);
        messagingService.sendToUser(user, new Message().setType(
                MessageType.EXPORT_START).setPayload(Json.serializeToString(exportDao.get(export.getId()))));

        try {

            /*
             * Initialize all the processors
             */
            List<ExportOutput> allOutputs = exportOutputDao.getAll(export);
            ExportOutput aggregatorOutput = new ExportOutput();
            aggregatorOutput.setFactory(new ProcessorFactory<>(ExportDateAggregator.class));
            allOutputs.add(aggregatorOutput);

            for (ExportOutput output: allOutputs) {

                ExportProcessor processor = output.getFactory().newInstance();
                if (output.getPath() != null) {
                    // Create a working directory for each output
                    FileUtils.makedirs(output.getDirName());
                }
                AutowireCapableBeanFactory autowire = applicationContext.getAutowireCapableBeanFactory();
                autowire.autowireBean(processor);

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
                assetCount++;

                for (Map.Entry<ExportOutput, ExportProcessor> entry : outputs.entrySet()) {
                    ExportProcessor processor = entry.getValue();
                    ExportOutput output = entry.getKey();

                    try {
                        processor.process(exportOptionsService.applyOptions(export, output, asset));
                        assetDao.addToExport(asset, export);
                        messagingService.sendToUser(user, new Message().setType(
                                MessageType.EXPORT_ASSET).setPayload(
                                    ImmutableMap.of("assetId", asset.getId(), "exportId", export.getId())));

                    } catch (Exception e) {
                        /*
                         * exportOptionsService.applyOptions may throw an exception if there
                         * is an error processing the source data.
                         */
                        logger.warn("Failed to add asset {} to output '{}',", asset, e);
                    }

                    if (exportDao.isInState(export, ExportState.Cancelled)) {
                        logger.warn("Export {} was cancelled", export);
                        break;
                    }
                }
            }

            /*
             * For cancelled exports we still go through tear downs.
             */
            for (Map.Entry<ExportOutput, ExportProcessor> entry: outputs.entrySet()) {
                ExportProcessor processor = entry.getValue();
                ExportOutput output = entry.getKey();

                logger.info("tearing down processor {}", processor);
                try {
                    processor.teardown();

                    /**
                     * Set the exports as online if the path exists
                     */
                    if (output.pathExists()) {
                        exportOutputDao.setOnline(output);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to tear down processor '{}',", processor, e);
                }

                messagingService.sendToUser(user, new Message().setType(
                        MessageType.EXPORT_OUTPUT_STOP).setPayload(Json.serializeToString(output)));
            }

        } finally {
            /*
             * Cancelled exports don't get set to finished, they state in the canceled state.
             */
            if (exportDao.setFinished(export)) {
                logger.info("Export ID:{} complete, {} assets exported.", export.getId(), assetCount);
                messagingService.sendToUser(user, new Message().setType(
                        MessageType.EXPORT_STOP).setPayload(Json.serializeToString(exportDao.get(export.getId()))));
            }
            else if (exportDao.isInState(export, ExportState.Cancelled)) {
                /**
                 * Offline any files we might have generated right away.
                 */
                exportService.offline(export);
            }

            /**
             * Logs the user out.
             */
            SecurityContextHolder.getContext().setAuthentication(null);
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

        /**
         * Check for expired outputs.
         */
        for (ExportOutput output: exportOutputDao.getAllExpired(
                StringUtil.durationToMillis(expireTime))) {
            exportService.offline(output);
        }
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedDelaySchedule(10, 1, TimeUnit.SECONDS);
    }
}
