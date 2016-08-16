package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.ImportSpec;
import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.domain.JobFilter;
import com.zorroa.archivist.domain.PipelineType;
import com.zorroa.archivist.repository.JobDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.plugins.Module;
import com.zorroa.sdk.plugins.ModuleRef;
import com.zorroa.sdk.processor.ProcessorSpec;
import com.zorroa.sdk.zps.ZpsScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Override
    public PagedList<Job> getAll(Paging page) {
        return jobService.getAll(page, new JobFilter().setType(PipelineType.Import));
    }

    @Override
    public Job create(ImportSpec spec) {

        ZpsScript script = new ZpsScript();
        script.setJobId(jobDao.nextId());

        if (spec.getName() == null) {
            script.setName(String.format("import by %s", SecurityUtils.getUsername()));
        }
        else {
            script.setName(String.format("import ", spec.getName()));
        }

        /*
         * Validate the processors
         */
        List<ProcessorSpec> pipeline = Lists.newArrayList();
        if (spec.getPipelineId() != null) {
            for (ModuleRef m: pipelineService.get(spec.getPipelineId()).getProcessors()) {
                Module module = pluginService.getModule(m.getName());
                pipeline.add(module.getProcessorSpec(m.getArgs()));
            }
        }
        pipeline.add(
                new ProcessorSpec()
                        .setClassName("com.zorroa.sdk.processor.builtin.IndexSource")
                        .setLanguage("java")
                        .setPlugin("builtin")
                        .setArgs(ImmutableMap.of("importId", script.getJobId())));

        /**
         * The pipeline is attached to the generator.
         */
        List<ProcessorSpec> generators = Lists.newArrayListWithCapacity(spec.getGenerators().size());
        for (ModuleRef m: spec.getGenerators()) {
            Module module = pluginService.getModule(m.getName());
            ProcessorSpec generator = module.getProcessorSpec(m.getArgs());
            generator.getArgs().put("pipeline", pipeline);
            generators.add(module.getProcessorSpec(m.getArgs()));
        }

        /**
         * The execute property holds the current processors to be executed.
         */
        script.setExecute(generators);

        jobService.launch(script, Import);
        Job job = jobService.get(script.getJobId());
        return job;
    }
}
