package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.repository.ExportDao;
import com.zorroa.archivist.repository.JobDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.domain.Document;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.processor.ExportArgs;
import com.zorroa.sdk.processor.PipelineType;
import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.search.AssetFilter;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.zps.ZpsScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;

/**
 * Created by chambers on 7/8/16.
 */
@Service
@Transactional
public class ExportServiceImpl implements ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportServiceImpl.class);

    @Autowired
    JobService jobService;

    @Autowired
    JobDao jobDao;

    @Autowired
    ExportDao exportDao;

    @Autowired
    PipelineService pipelineService;

    @Autowired
    PluginService pluginService;

    @Autowired
    SearchService searchService;

    @Autowired
    AssetDao assetDao;

    @Autowired
    ApplicationProperties properties;

    @Autowired
    TransactionEventManager transactionEventManager;

    @Autowired
    EventLogService logService;

    @Value("${archivist.export.priority}")
    int taskPriority;

    /**
     * A temporary place to stuff parameters detected when the search
     * is generated.
     */
    private class ExportParams {

        AssetSearch search;

        boolean pages = false;

        boolean frames = false;
    }

    /*
     * Perform the export search to tag all the assets being exported.  We then
     * replace the the search being executed with a search for the assets
     * we specifically tagged.
     */
    private ExportParams performExportSearch(AssetSearch search, int exportId) {
        search.setFields(new String[] {"source"} );

        ExportParams params = new ExportParams();
        List<String> ids = Lists.newArrayListWithCapacity(64);
        for (Document asset : searchService.scanAndScroll(search,
                properties.getInt("archivist.export.maxAssetCount"))) {
            ids.add(asset.getId());

            // Temp stuff
            if (asset.attrExists("source.clip.page")) {
                params.pages = true;
            }
            if (asset.attrExists("source.clip.frame")) {
                params.frames = true;
            }
        }

        if (ids.isEmpty()) {
            throw new ArchivistWriteException("Unable to start export, search returns no assets");
        }

        assetDao.appendLink("export", String.valueOf(exportId), ids);
        params.search = new AssetSearch().setFilter(
                new AssetFilter().addToLinks("export", String.valueOf(exportId)));
        return params;
    }

    @Override
    public ExportFile createExportFile(Job job, ExportFileSpec spec) {
        return exportDao.createExportFile(job, spec);
    }

    @Override
    public ExportFile getExportFile(long id) {
        return exportDao.getExportFile(id);
    }

    @Override
    public List<ExportFile> getAllExportFiles(Job job) {
        return exportDao.getAllExportFiles(job);
    }

    @Override
    public Job create(ExportSpec spec) {

        JobSpec jspec = new JobSpec();
        jspec.setType(PipelineType.Export);

        if (spec.getName() == null) {
            jspec.setName(String.format("export by %s", SecurityUtils.getUsername()));
        }
        else {
            jspec.setName(spec.getName());
        }

        jobDao.nextId(jspec);
        Path jobRoot = jobService.resolveJobRoot(jspec);

        Job job = jobService.launch(jspec);

        /**
         * Now start to build the script for the task.
         */
        ZpsScript script = new ZpsScript();
        script.putToGlobals("exportArgs", new ExportArgs()
                        .setExportId(jspec.getJobId())
                        .setExportName(jspec.getName())
                        .setExportRoot(jobRoot.resolve("exported").toString()));

        /**
         * This entire job runs in a single frame.  If this is eventually set False
         * to do parallel processing, the whole pipeline has to be reworked.
         */
        script.setInline(true);

        /**
         * Arrays for the primary and per-asset pipeline.
         */
        List<ProcessorRef> generate = Lists.newArrayList();
        List<ProcessorRef> execute = pipelineService.mungePipelines(PipelineType.Export, spec.getProcessors());

        script.setGenerate(generate);
        script.setExecute(execute);

        /**
         * Replace the search the user supplied with our own search so we ensure
         * we get the exact assets during the export and new data
         * added that might match their search change the export.
         */
        ExportParams params = performExportSearch(spec.getSearch(), job.getJobId());
        generate.add(pluginService.getProcessorRef(
                "com.zorroa.core.generator.AssetSearchGenerator",
                ImmutableMap.of("search", params.search)));

        /**
         * Add the collector which registers ouputs with the server.
         */
        execute.add(pluginService.getProcessorRef("com.zorroa.core.collector.ExportCollector"));

        jobService.createTask(new TaskSpec()
                .setJobId(job.getJobId())
                .setName("Setup and Generation")
                .setOrder(taskPriority)
                .setScript(script));

        /**
         * Log the create export with the search for the given assets.  When someone
         * downloads the export, that actually logs it as an exported asset.
         */
        transactionEventManager.afterCommitSync(() -> {
            logService.logAsync(UserLogSpec.build(LogAction.Create,
                    "export", job.getJobId()));
        });

        return job;
    }

    @Override
    public PagedList<Job> getAll(Pager page) {
        return jobService.getAll(page, new JobFilter().setType(PipelineType.Export));
    }
}
