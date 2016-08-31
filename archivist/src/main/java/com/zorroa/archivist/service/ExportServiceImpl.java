package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.JobDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.exception.ZorroaWriteException;
import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.processor.SharedData;
import com.zorroa.sdk.search.AssetFilter;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.zps.ZpsScript;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.List;

/**
 * Created by chambers on 7/8/16.
 */
@Service
@Transactional
public class ExportServiceImpl implements ExportService {

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

    SharedData sharedData;

    @PostConstruct
    public void init() {
        /**
         * Create a shared data which contains all the cluster properties.
         */
        sharedData = SharedData.builder()
            .setRootPath(properties.getString("zorroa.cluster.path.shared"))
            .setModelPath(properties.getString("zorroa.cluster.path.models"))
            .setOfsPath(properties.getString("zorroa.cluster.path.ofs"))
            .setPluginPath(properties.getString("zorroa.cluster.path.plugins"))
            .setExportPath(properties.getString("zorroa.cluster.path.exports"))
            .build();
    }

    /*
     * Perform the export search to tag all the assets being exported.  We then
     * replace the the search being executed with a search for the assets
     * we specifically tagged.
     */
    private AssetSearch performExportSearch(AssetSearch search, int exportId) {
        search.setFields(new String[] {"source"} );

        List<String> ids = Lists.newArrayListWithCapacity(64);
        for (Asset asset : searchService.scanAndScroll(search,
                properties.getInt("archivist.export.maxAssetCount"))) {
            ids.add(asset.getId());
        }

        if (ids.isEmpty()) {
            throw new ZorroaWriteException("Unable to start export, search returns no assets");
        }
        assetDao.appendLink("export", String.valueOf(exportId), ids);
        return new AssetSearch().setFilter(
                new AssetFilter().addToTerms("link.export.id", String.valueOf(exportId)));
    }

    @Override
    public Job create(ExportSpec spec) {

        JobSpec jspec = new JobSpec();
        jspec.setType(PipelineType.Export);
        if (spec.getName() == null) {
            jspec.setName(String.format("export by %s", SecurityUtils.getUsername()));
        }
        else {
            jspec.setName(String.format("export %s ", spec.getName()));
        }

        jobDao.nextId(jspec);
        Path exportRoot = getExportPath(jspec);

        jspec.putToArgs("exportId", jspec.getJobId());
        jspec.putToArgs("outputFile", exportRoot.toString());
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
        List<ProcessorRef> export = Lists.newArrayList();
        List<ProcessorRef> pipeline = Lists.newArrayList();
        script.setExecute(pipeline);

        /**
         * First setup the core pipeline for the main task.  We create the
         * output directory, add the export id, and kick off a search generator.
         */
        pipeline.add(new SdkProcessorRef()
                .setClassName("com.zorroa.sdk.processor.builtin.MakeDirectory")
                .setLanguage("java")
                .setArgs(ImmutableMap.of("path", exportRoot.toString())));

        /**
         * Replace the search the user supplied with our own search so we ensure
         * we get the exact assets during the export and new data
         * added that might match their search change the export.
         */
        AssetSearch search = performExportSearch(spec.getSearch(), job.getJobId());
        pipeline.add(pluginService.getProcessorRef(
                "com.zorroa.sdk.processor.builtin.AssetSearchGenerator",
                ImmutableMap.of(
                        "pipeline", export,
                        "search", search)));

        /**
         * Now setup the per file export pipeline.  A CopySource processors is
         * prepended in case we need to modify the original source.  Then any
         * modifications by the user.  Finally a CompressSource is appended.  All
         * of this is run inline to the generator.
         */
        export.add(new SdkProcessorRef()
                .setClassName("com.zorroa.sdk.processor.builtin.CopySource")
                .setLanguage("java")
                .setArgs(ImmutableMap.of("dstDirectory", exportRoot.resolve("tmp").toString())));

        if (spec.getPipelineId() != null) {
            for (ProcessorRef ref: pipelineService.get(spec.getPipelineId()).getProcessors()) {
                export.add(pluginService.getProcessorRef(ref));
            }
        }

        Path zipFile = exportRoot.resolve("zorroa_export_v" + job.getJobId() + ".zip");
        export.add(new SdkProcessorRef()
                .setClassName("com.zorroa.sdk.processor.builtin.CompressSource")
                .setLanguage("java")
                .setArgs(ImmutableMap.of("dstFile", zipFile.toString())));

        jobService.createTask(new TaskSpec()
                .setJobId(job.getJobId())
                .setName("Setup and Generation")
                .setScript(script));

        /**
         * Log the create export with the search for the given assets.  When someone
         * downloads the export, that actually logs it as an exported asset.
         */
        transactionEventManager.afterCommitSync(() -> {
            logService.log(LogSpec.build(LogAction.Create,
                    "export", job.getId()).setSearch(search));
        });

        return job;
    }

    @Override
    public PagedList<Job> getAll(Paging page) {
        return jobService.getAll(page, new JobFilter().setType(PipelineType.Export));
    }

    private Path getExportPath(JobSpec spec) {
        DateTime time = new DateTime();
        DateTimeFormatter formatter = DateTimeFormat.forPattern("YYYY-MM-dd");

        return sharedData.getExportPath()
                .resolve(formatter.print(time))
                .resolve(String.valueOf(spec.getJobId()));

    }
}
