package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.JobDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
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
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    @Autowired
    ApplicationProperties properties;

    List<String> pathSuggestFilter = Lists.newArrayList();

    @PostConstruct
    public void init() {
        Map<String, Object> vals = properties.getMap("archivist.import.suggest.paths");
        if (vals != null) {
            for (Map.Entry<String,Object> entry: vals.entrySet()) {
                String path = FileUtils.normalize((String) entry.getValue());
                pathSuggestFilter.add(path);
                logger.info("Allowing Imports from '{}' {}", entry.getKey(), entry.getValue());
            }
        }
    }

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
    public Job create(UploadImportSpec spec) {

        JobSpec jspec = new JobSpec();
        jspec.setType(Import);
        jspec.setName(determineJobName(spec.getName()));

        Job job = jobService.launch(jspec);

        // Setup generator
        List<ProcessorRef> generators = Lists.newArrayList();
        try {
            Path importPath = copyUploadedFiles(job, spec.getFiles());
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
        /*
         * Get the processors for the user supplied pipeline if.
         */
        List<ProcessorRef> pipeline = pipelineService.getProcessors(
                spec.getPipelineId(), ImmutableList.of());

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
                .setName("Generation via " + generators.get(0).getClassName()));

        transactionEventManager.afterCommitSync(() -> {
            logService.logAsync(LogSpec.build(LogAction.Create, "import", job.getJobId()));
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
                .setName("Frame Generator"));

        transactionEventManager.afterCommitSync(() -> {
            logService.logAsync(LogSpec.build(LogAction.Create, "import", job.getJobId()));
        });

        return job;
    }

    @Override
    public Map<String, List<String>> suggestImportPath(String path) {
        Map<String, List<String>> result = ImmutableMap.of(
                "dirs", Lists.newArrayList(),
                "files", Lists.newArrayList());

        /*
         * Gotta normalize it since we allow relative paths for testing purposes.
         */
        path = FileUtils.normalize(path);
        if (!isPathAllowed(path)) {
            return result;
        }

        /*
         * If there are no filters, we don't allow anything to be returned.
         * This is the secure default option.
        */

        try {
            for (File f : new File(path).listFiles()) {
                if (f.isHidden()) {
                    continue;
                }
                String t = f.isDirectory() ? "dirs" : "files";
                result.get(t).add(f.getName());
            }
        } catch (Exception e) {
            return result;
        }

        Collections.sort(result.get("dirs"));
        Collections.sort(result.get("files"));
        return result;
    }


    public boolean isPathAllowed(String path) {

        if (pathSuggestFilter.isEmpty()) {
            return false;
        }
        else {
            boolean matched = false;
            for (String filter: pathSuggestFilter) {
                if (path.startsWith(filter)) {
                    matched = true;
                    break;
                }
            }
            return matched;
        }
    }

    private Path copyUploadedFiles(Job job, List<MultipartFile> files) throws IOException {
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
