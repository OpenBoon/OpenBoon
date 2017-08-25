package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.zps.ZpsScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    PipelineService pipelineService;

    @Autowired
    PluginService pluginService;

    @Autowired
    TransactionEventManager transactionEventManager;

    @Autowired
    EventLogService logService;

    @Autowired
    ApplicationProperties properties;

    @Value("${archivist.import.priority}")
    int taskPriority;

    @Override
    public PagedList<Job> getAll(Pager page) {
        return jobService.getAll(page, new JobFilter().setType(PipelineType.Import));
    }

    @Override
    public Job create(UploadImportSpec spec, MultipartFile[] files) {

        JobSpec jspec = new JobSpec();
        jspec.setType(Import);
        jspec.setName(determineJobName(spec.getName()));

        Job job = jobService.launch(jspec);

        // Setup generator
        List<ProcessorRef> generators = Lists.newArrayList();
        try {
            Path importPath = copyUploadedFiles(job, files);
            generators.add(new ProcessorRef()
                    .setClassName("com.zorroa.core.generator.FileSystemGenerator")
                    .setLanguage("java")
                    .setArg("path", importPath.toString()));

        } catch (IOException e) {
            logger.warn("Failed to copy uploaded files:", e);
            throw new ArchivistWriteException("Failed to copy uploaded files, unexpected :" + e.getMessage());
        }

        // Setup execute
        List<ProcessorRef> execute = Lists.newArrayList();

        /*
         * The first node is an expand collector which allows us to execute in parallel.
         */
        execute.add(
                new ProcessorRef()
                        .setClassName("com.zorroa.core.collector.ExpandCollector")
                        .setLanguage("java"));


        List<ProcessorRef> pipeline = Lists.newArrayList();
        pipeline.addAll(pipelineService.mungePipelines(spec.getPipelineIds(), null));

        /*
         * Append the index document collector to add stuff to the DB.
         */
        pipeline.add(
                new ProcessorRef()
                        .setClassName("com.zorroa.core.collector.IndexDocumentCollector")
                        .setLanguage("java")
                        .setArgs(ImmutableMap.of("importId", job.getJobId())));

        /*
         * Set the pipeline as the sub execute to the expand node.
         */
        execute.get(0).setExecute(pipeline);

        /*
         * Now build the script.
         */
        ZpsScript script = new ZpsScript();
        script.setGenerate(generators);
        script.setExecute(execute);

        jobService.createTask(new TaskSpec().setScript(script)
                .setJobId(job.getJobId())
                .setOrder(taskPriority)
                .setName("Generation via " + generators.get(0).getClassName()));

        transactionEventManager.afterCommitSync(() -> {
            logService.logAsync(UserLogSpec.build(LogAction.Create, "import", job.getJobId()));
        });

        return job;
    }

    @Override
    public Job create(ImportSpec spec) {
        JobSpec jspec = new JobSpec();
        jspec.setType(Import);
        jspec.setName(determineJobName(spec.getName()));

        /**
         * Create the job.
         */
        Job job = jobService.launch(jspec);


        ProcessorRef expand = pluginService.getProcessorRef("com.zorroa.core.collector.ExpandCollector");
        List<ProcessorRef> execute = Lists.newArrayList(expand);
        expand.setExecute(pipelineService.mungePipelines(spec.getPipelineIds(), spec.getProcessors()));

        /**
         * At the end we add an IndexDocumentCollector to index the results of our job.
         */
        expand.addToExecute(
                new ProcessorRef()
                        .setClassName("com.zorroa.core.collector.IndexDocumentCollector")
                        .setLanguage("java")
                        .setArgs(ImmutableMap.of("importId", job.getJobId())));

        /**
         * Now attach the pipeline to each generator, be sure to validate each processor
         * since they are coming from the user.
         */
        List<ProcessorRef> generators = Lists.newArrayList();
        if (spec.getGenerators() != null) {
            for (ProcessorRef m : spec.getGenerators()) {
                ProcessorRef gen = pluginService.getProcessorRef(m);
                generators.add(gen);
            }
        }

        /**
         * The execute property holds the current processors to be executed.
         */
        ZpsScript script = new ZpsScript();
        script.setGenerate(generators);
        script.setExecute(execute);

        jobService.createTask(new TaskSpec().setScript(script)
                .setJobId(job.getJobId())
                .setOrder(taskPriority)
                .setName("Frame Generator"));

        transactionEventManager.afterCommitSync(() -> {
            logService.logAsync(UserLogSpec.build(LogAction.Create, "import", job.getJobId()));
        });

        return job;
    }

    private Path copyUploadedFiles(Job job, MultipartFile[] files) throws IOException {
        Path importPath = Paths.get(job.getRootPath()).resolve("assets");

        for (MultipartFile file: files) {
            if (!importPath.resolve(file.getOriginalFilename()).toFile().exists()) {
                Files.copy(file.getInputStream(), importPath.resolve(file.getOriginalFilename()));
            }
        }
        return importPath;
    }

    private String determineJobName(String name) {
        if (name == null) {
            return String.format("import by %s", SecurityUtils.getUsername());
        }
        else {
            return name;
        }
    }
}
