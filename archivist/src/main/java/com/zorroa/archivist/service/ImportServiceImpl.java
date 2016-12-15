package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.JobDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.util.FileUtils;
import com.zorroa.sdk.zps.ZpsScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.zorroa.archivist.domain.PipelineType.Import;

/**
 * ImportService provides a simple interface for making Import jobs.
 * An Import itself is just a job running an import pipeline.
 */
@Service
@Transactional
public class ImportServiceImpl implements ImportService {

    private static final Logger logger = LoggerFactory.getLogger(ImportServiceImpl.class);

    @Autowired
    JobService jobService;

    @Autowired
    JobDao jobDao;

    @Autowired
    PipelineService pipelineService;

    @Autowired
    PluginService pluginService;

    @Autowired
    TransactionEventManager transactionEventManager;

    @Autowired
    LogService logService;

    @Override
    public PagedList<Job> getAll(Pager page) {
        return jobService.getAll(page, new JobFilter().setType(PipelineType.Import));
    }

    @Override
    public Job create(DebugImportSpec spec) {
        String syncId = UUID.randomUUID().toString();
        JobSpec jspec = new JobSpec();
        jspec.putToArgs("syncId", syncId);
        jspec.setType(Import);

        List<ProcessorRef> generator;
        if (JdbcUtils.isValid(spec.getPath())) {
            jspec.setName(String.format("debugging import by %s (file=%s)",
                    SecurityUtils.getUsername(), FileUtils.filename(spec.getPath())));

            generator = ImmutableList.of(
                    new ProcessorRef("com.zorroa.core.generator.FileListGenerator")
                            .setArg("paths", ImmutableList.of(spec.getPath())));
        }
        else if (JdbcUtils.isValid(spec.getQuery())) {
            jspec.setName(String.format("debugging import by %s (search=%s)",
                    SecurityUtils.getUsername(), spec.getQuery()));

            AssetSearch search = new AssetSearch();
            search.setQuery(spec.getQuery());
            search.setSize(1);
            generator = ImmutableList.of(
                    new ProcessorRef("com.zorroa.core.generator.AssetSearchGenerator")
                            .setArg("search", search));
        }
        else {
            throw new IllegalArgumentException("Must set either a path or search query.");
        }

        List<ProcessorRef> pipeline = pipelineService.getProcessors(
                spec.getPipelineId(), spec.getPipeline());
        pipeline.add(new ProcessorRef("com.zorroa.core.processor.ReturnResponse"));

        ZpsScript script = new ZpsScript();
        script.setGenerate(generator);
        script.setExecute(pipeline);
        script.setInline(true);
        script.setStrict(true);

        Job job = jobService.launch(jspec);
        jobService.createTask(new TaskSpec().setScript(script)
                .setJobId(job.getJobId())
                .setName("Path Generation"));


        return job;
    }

    @Override
    public Job create(ImportSpec spec) {

        JobSpec jspec = new JobSpec();
        jspec.setType(Import);
        if (spec.getName() == null) {
            jspec.setName(String.format("import by %s", SecurityUtils.getUsername()));
        }
        else {
            jspec.setName(String.format("import ", spec.getName()));
        }

        /**
         * Create the job.
         */
        Job job = jobService.launch(jspec);

        List<ProcessorRef> execute = Lists.newArrayList();

        /**
         * Add an ExpandCollector so we generate right into new tasks.
         */
        execute.add(
                new ProcessorRef()
                        .setClassName("com.zorroa.core.collector.ExpandCollector")
                        .setLanguage("java"));

        /**
         * Resolve the user supplied pipeline.
         */
        List<ProcessorRef> pipeline = pipelineService.getProcessors(
                spec.getPipelineId(), spec.getPipeline());
        /**
         * At the end we add an IndexDocumentCollector to index the results of our job.
         */
        pipeline.add(
                new ProcessorRef()
                        .setClassName("com.zorroa.core.collector.IndexDocumentCollector")
                        .setLanguage("java")
                        .setArgs(ImmutableMap.of("importId", job.getJobId())));

        /**
         * Now finally, attach the pipeline to the expander as a sub execute list.
         */
        execute.get(0).setExecute(pipeline);

        /**
         * Now attach the pipeline to each generator, be sure to validate each processor
         * since they are coming from the user.
         */
        List<ProcessorRef> generators = Lists.newArrayListWithCapacity(spec.getGenerators().size());
        for (ProcessorRef m: spec.getGenerators()) {
            ProcessorRef gen = pluginService.getProcessorRef(m);
            generators.add(gen);
        }

        /**
         * The execute property holds the current processors to be executed.
         */
        ZpsScript script = new ZpsScript();
        script.setGenerate(generators);
        script.setExecute(execute);

        jobService.createTask(new TaskSpec().setScript(script)
                .setJobId(job.getJobId())
                .setName("Frame Generator"));

        transactionEventManager.afterCommitSync(() -> {
            logService.logAsync(LogSpec.build(LogAction.Create, "import", job.getJobId()));
        });

        return job;
    }
}
