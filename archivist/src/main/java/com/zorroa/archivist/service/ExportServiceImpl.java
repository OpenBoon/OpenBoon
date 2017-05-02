package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.JobDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
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
    LogService logService;

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
        for (Asset asset : searchService.scanAndScroll(search,
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
        Path exportRoot = jobRoot.resolve("exported");
        Path zipFile = exportRoot.resolve(jspec.getName() + ".zip");

        jspec.putToArgs("exportId", jspec.getJobId());
        jspec.putToArgs("outputRoot", exportRoot.toString());
        jspec.putToArgs("outputFile", zipFile.toString());
        Job job = jobService.launch(jspec);

        /**
         * Now start to build the script for the task.
         */
        ZpsScript script = new ZpsScript();

        /**
         * This entire job runs in a single frame.  If this is eventually set False
         * to do parallel processing, the whole pipeline has to be reworked.
         */
        script.setInline(true);


        /**
         * Arrays for the primary and per-asset pipeline.
         */
        List<ProcessorRef> generate = Lists.newArrayList();
        List<ProcessorRef> execute = Lists.newArrayList();
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
         * Now setup the per file export pipeline.  A CopySource processors is
         * prepended in case we need to modify the original source.  Then any
         * modifications by the user.  Finally a CompressSource is appended.  All
         * of this is run inline to the generator.
         */
        String dstDirectory = exportRoot.resolve("tmp_" + System.currentTimeMillis()).toString();

        if (spec.isIncludeSource()) {
            execute.add(new ProcessorRef()
                    .setClassName("com.zorroa.core.processor.CopySource")
                    .setLanguage("java")
                    .setArgs(ImmutableMap.of("dstDirectory", dstDirectory)));
        }

        if (params.pages) {
            execute.add(new ProcessorRef()
                    .setClassName("com.zorroa.core.processor.PdfPageExporter")
                    .setLanguage("java")
                    .setArgs(ImmutableMap.of("dstFile",
                            dstDirectory  + "/" + jspec.getName() + ".pdf")));
        }

        if (spec.getPipelineId() != null) {
            for (ProcessorRef ref: pipelineService.get(spec.getPipelineId()).getProcessors()) {
                execute.add(pluginService.getProcessorRef(ref));
            }
        }

        if (spec.getFields() != null) {
            execute.add(new ProcessorRef()
                    .setClassName("com.zorroa.core.processor.MetadataExporter")
                    .setLanguage("java")
                    .setArgs(ImmutableMap.of("fields", spec.getFields(),
                            "dstFile", dstDirectory + "/" + jspec.getName() + ".csv")));
        }

        execute.add(new ProcessorRef()
                .setClassName("com.zorroa.core.processor.CompressDirectory")
                .setLanguage("java")
                .setArgs(ImmutableMap.of("dstFile", zipFile.toString(),
                        "srcDirectory", dstDirectory,
                        "removeSrcDirectory", true,
                        "zipEntryRoot", jspec.getName())));

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
            logService.logAsync(LogSpec.build(LogAction.Create,
                    "export", job.getJobId()));
        });

        return job;
    }

    @Override
    public PagedList<Job> getAll(Pager page) {
        return jobService.getAll(page, new JobFilter().setType(PipelineType.Export));
    }
}
