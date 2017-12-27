package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zorroa.archivist.domain.AnalyzeSpec;
import com.zorroa.common.cluster.client.ClusterConnectionException;
import com.zorroa.common.cluster.client.WorkerNodeClient;
import com.zorroa.common.cluster.thrift.StackElementT;
import com.zorroa.common.cluster.thrift.TaskErrorT;
import com.zorroa.common.cluster.thrift.TaskResultT;
import com.zorroa.common.cluster.thrift.TaskStartT;
import com.zorroa.common.config.NetworkEnvironment;
import com.zorroa.common.domain.Analyst;
import com.zorroa.sdk.client.exception.ArchivistException;
import com.zorroa.sdk.domain.Document;
import com.zorroa.sdk.filesystem.ObjectFile;
import com.zorroa.sdk.filesystem.ObjectFileSystem;
import com.zorroa.sdk.processor.PipelineType;
import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.processor.SharedData;
import com.zorroa.sdk.util.FileUtils;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.ZpsScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Created by chambers on 5/15/17.
 */
@Component
public class AnalyzeServiceImpl implements AnalyzeService {

    private static final Logger logger = LoggerFactory.getLogger(ImportServiceImpl.class);

    @Autowired
    AnalystService analystService;

    @Autowired
    SharedData sharedData;

    @Autowired
    ObjectFileSystem ofs;

    @Autowired
    AssetService assetService;

    @Autowired
    PipelineService pipelineService;

    @Autowired
    PluginService pluginService;

    @Autowired
    NetworkEnvironment network;

    @Override
    public Object analyze(AnalyzeSpec spec, MultipartFile[] files) throws IOException {

        ZpsScript script = new ZpsScript();
        script.setInline(true);
        script.setStrict(true);

        List<ProcessorRef> pipeline = Lists.newArrayList();
        pipeline.addAll(pipelineService.mungePipelines(PipelineType.Import, spec.getProcessors()));
        script.setExecute(pipeline);

        String lang;
        try {
            lang = script.getExecute().get(0).getLanguage();
        } catch (IndexOutOfBoundsException ignore) {
            // an empty pipeline, just ignore it
            lang = "java";
        }

        if (lang.equals("python")) {
            script.getExecute().add(pluginService.getProcessorRef("zorroa_py_core.document.PyReturnResponse"));
        }
        else {
            script.getExecute().add(pluginService.getProcessorRef("com.zorroa.core.processor.ReturnResponse"));
        }

        if (files != null) {
            if (lang.equals("python")) {
                script.setGenerate(ImmutableList.of(new ProcessorRef()
                        .setClassName("zorroa_py_core.generators.PyFileGenerator")
                        .setLanguage("python")
                        .setArg("paths", copyUploadedFiles(files))));

            } else {
                script.setGenerate(ImmutableList.of(new ProcessorRef()
                        .setClassName("com.zorroa.core.generator.FileListGenerator")
                        .setLanguage("java")
                        .setArg("paths", copyUploadedFiles(files))));
            }
        }
        else if (spec.getAsset() != null) {
            Document asset = assetService.get(spec.getAsset());
            script.setOver(ImmutableList.of(asset));
        }
        else {
            throw new ArchivistException("No file or asset specified");
        }

        List<Analyst> analysts = analystService.getActive();
        if (analysts.isEmpty()) {
            throw new ArchivistException("Unable to find a suitable analyst.");
        }

        for (Analyst analyst: analysts) {
            try {
                WorkerNodeClient client = new WorkerNodeClient(analyst.getUrl());
                // Never retry so we don't accidentally run the same command.
                client.setMaxRetries(0);
                client.setConnectTimeout(1000);
                // Wait up to 120 seconds for result.
                client.setSocketTimeout(120 * 1000);

                TaskResultT resultT = client.executeTask(new TaskStartT()
                        .setArgMap(Json.serialize(spec.getArgs()))
                        .setEnv(ImmutableMap.of())
                        .setMasterHost(network.getClusterAddr())
                        .setName("execute")
                        .setOrder(-1000)
                        .setSharedDir(sharedData.getRoot().toString())
                        .setScript(Json.serialize(script)));

                if (resultT.getResult() != null) {
                    // The ReturnResponse object returns a list.
                    return ImmutableMap.of("list",
                            Json.deserialize(resultT.getResult(), List.class),
                            "errors", makeRestFriendly(resultT.getErrors()));
                }
                else {
                    return ImmutableMap.of(
                            "errors", makeRestFriendly(resultT.getErrors()));
                }

            } catch (ClusterConnectionException e) {
                logger.warn("Unable to connect to analyst: {}", e.getMessage());
            }
        }

        throw new ArchivistException("All analysts timed out.");
    }

    private List<String> copyUploadedFiles(MultipartFile[] files) throws IOException {
        List<String> result = Lists.newArrayListWithCapacity(files.length);
        for (MultipartFile file: files) {
            ObjectFile ofile = ofs.prepare("tmp", file.getOriginalFilename() + file.getSize(),
                    FileUtils.extension(file.getOriginalFilename()));
            Path path = ofile.getFile().toPath();

            if (!ofile.exists()) {
                Files.copy(file.getInputStream(), path);
            }
            result.add(path.toString());
        }
        return result;
    }

    private List<Map<String,Object>> makeRestFriendly(List<TaskErrorT> errors) {
        List<Map<String,Object>> result = Lists.newArrayListWithCapacity(errors.size());
        for (TaskErrorT error: errors) {
            Map<String, Object> entry = Maps.newHashMap();
            entry.put("message", error.getMessage());
            entry.put("service", error.getOriginService());
            entry.put("skipped", error.isSkipped());
            entry.put("path", error.getPath());
            entry.put("assetId", error.getId());
            entry.put("processor", error.getProcessor());
            entry.put("phase", error.getPhase());
            entry.put("timestamp", error.getTimestamp());

            List<Map<String,Object>> stackTrace = Lists.newArrayList();
            entry.put("stackTrace", stackTrace);

            for (StackElementT e: error.getStack()) {
                Map<String,Object> stack = Maps.newHashMap();
                stack.put("className", e.getClassName());
                stack.put("file", e.getFile());
                stack.put("lineNumber", e.getLineNumber());
                stack.put("method", e.getMethod());
                stackTrace.add(stack);
            }
            result.add(entry);
        }
        return result;
    }

}
