package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.JobDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.processor.ProcessorRef;
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
    public PagedList<Job> getAll(Paging page) {
        return jobService.getAll(page, new JobFilter().setType(PipelineType.Import));
    }

    @Override
    public Job create(DebugImportSpec spec) {
        String syncId = UUID.randomUUID().toString();

        JobSpec jspec = new JobSpec();
        jspec.putToArgs("syncId", syncId);
        jspec.setType(Import);
        jspec.setName(String.format("debugging import by %s (%s)",
                SecurityUtils.getUsername(), FileUtils.filename(spec.getPath())));

        Job job = jobService.launch(jspec);

        List<ProcessorRef> pipeline = pipelineService.getProcessors(
                spec.getPipelineId(), spec.getPipeline());

        /*
         * Add the final processor which ships the data back to this waiting call.
         */
        pipeline.add(new SdkProcessorRef("com.zorroa.sdk.processor.builtin.ReturnResponse"));

        List<ProcessorRef> generator = Lists.newArrayList();
        generator.add(
                new SdkProcessorRef("com.zorroa.sdk.processor.builtin.FileListGenerator")
                .setArg("paths", ImmutableList.of(spec.getPath()))
                .setArg("pipeline", pipeline));

        ZpsScript script = new ZpsScript();
        script.setExecute(generator);
        script.setInline(true);

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

        List<ProcessorRef> pipeline = pipelineService.getProcessors(
                spec.getPipelineId(), spec.getPipeline());

        /**
         * At the end we add an IndexSource processor.
         */
        pipeline.add(
                new SdkProcessorRef()
                        .setClassName("com.zorroa.sdk.processor.builtin.IndexSource")
                        .setLanguage("java")
                        .setArgs(ImmutableMap.of("importId", job.getJobId())));

        /**
         * Now attach the pipeline to each generator, be sure to validate each processor
         * since they are coming from the user.
         */
        List<ProcessorRef> generators = Lists.newArrayListWithCapacity(spec.getGenerators().size());
        for (ProcessorRef m: spec.getGenerators()) {
            ProcessorRef gen = pluginService.getProcessorRef(m);
            gen.setArg("pipeline", pipeline);
            generators.add(gen);
        }

        /**
         * The execute property holds the current processors to be executed.
         */
        ZpsScript script = new ZpsScript();
        script.setExecute(generators);

        jobService.createTask(new TaskSpec().setScript(script)
                .setJobId(job.getJobId())
                .setName("Path Generation"));

        transactionEventManager.afterCommitSync(() -> {
            logService.log(LogSpec.build(LogAction.Create, "import", job.getJobId()));
        });

        return job;
    }
}
