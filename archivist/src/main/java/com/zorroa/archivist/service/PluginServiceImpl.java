package com.zorroa.archivist.service;

import com.google.common.collect.Maps;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.PipelineDao;
import com.zorroa.archivist.repository.PluginDao;
import com.zorroa.archivist.repository.ProcessorDao;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.exception.PluginException;
import com.zorroa.sdk.plugins.*;
import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.processor.SharedData;
import com.zorroa.sdk.util.FileUtils;
import com.zorroa.sdk.util.StringUtils;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Responsible for installing and registering plugins with elastic.
 */
@Service
@Transactional
public class PluginServiceImpl implements PluginService {

    private static final Logger logger = LoggerFactory.getLogger(PluginServiceImpl.class);

    @Autowired
    ApplicationProperties properties;

    @Autowired
    PluginDao pluginDao;

    @Autowired
    ProcessorDao processorDao;

    @Autowired
    PipelineDao pipelineDao;

    @Autowired
    Client client;

    @Autowired
    SharedData sharedData;

    @Value("${zorroa.cluster.index.alias}")
    private String alias;

    @Value("${archivist.path.models}")
    private String modelPathStr;

    PluginInstaller pluginInstaller;

    PluginLoader pluginLoader;

    Path pluginPath;
    Path modelPath;

    @PostConstruct
    public void init() {
        String _path = properties.getString("archivist.path.plugins");
        if (_path.contains(":")) {
            throw new RuntimeException("Cannot specify multiple plugin paths.");
        }
        pluginPath =  FileUtils.normalize(Paths.get(_path));
        modelPath = FileUtils.normalize(Paths.get(modelPathStr));

        logger.info("Loading plugins from: {}", pluginPath);
        if (pluginPath.toFile().mkdirs()) {
            logger.info("Plugin path did not exist: {}", pluginPath);
        }

        pluginInstaller = new PluginInstaller(sharedData);
        pluginLoader = new PluginLoader(sharedData.getPluginPath());
    }

    @Override
    public Plugin installPlugin(MultipartFile file) {
        synchronized(pluginInstaller) {
            Path dst = pluginPath.resolve(file.getOriginalFilename());
            try {
                Files.copy(file.getInputStream(), dst);
                Path pluginPath = pluginInstaller.unpackPluginPackage(dst);
                PluginSpec plugin = pluginLoader.loadPlugin(pluginPath);
                installPlugin(plugin);
                return pluginDao.get(plugin.getName());

            } catch (Exception e) {
                throw new PluginException("Failed to install plugin, " + e.getMessage(), e);
            }
        }
    }

    @Override
    public Plugin installPlugin(Path zipFilePath) {
        synchronized(pluginLoader) {
            if (zipFilePath.startsWith(pluginPath)) {
                throw new PluginException("Plugin '" + zipFilePath + "' is already installed.");
            }

            Path dst = pluginPath.resolve(FileUtils.filename(zipFilePath));
            try {
                Files.copy(zipFilePath, dst);
                Path pluginPath = pluginInstaller.unpackPluginPackage(dst);
                PluginSpec plugin = pluginLoader.loadPlugin(pluginPath);
                installPlugin(plugin);
                return pluginDao.get(plugin.getName());
            } catch (Exception e) {
                throw new PluginException("Failed to install plugin, " + e.getMessage(), e);
            }
        }
    }

    private void installPlugin(PluginSpec spec) {
        boolean newOrChanged = false;

        Plugin plugin;
        try {
            plugin = pluginDao.get(spec.getName());
            if (!plugin.getVersion().equals(spec.getVersion())) {
                newOrChanged = true;
                pluginDao.update(plugin.getId(), spec);
            }
        } catch (EmptyResultDataAccessException e) {
            newOrChanged = true;
            plugin = pluginDao.create(spec);
        }

        /**
         * If the plugin is a new version or just a new plugin then we register
         * the other stuff.
         *
         * READ: On version up, existing entities created by a plugin are DELETED.
         * TODO: Save old entities as .old?
         */
        if (newOrChanged) {
            registerProcessors(plugin, spec);
            registerPipelines(plugin, spec);
        }
    }

    public void registerPipelines(Plugin p, PluginSpec spec) {
        if (spec.getPipelines() == null) {
            return;
        }

        for (PipelineSpec pl: spec.getPipelines()) {
            Pipeline pipeline;
            try {
                // remove the old one
                pipeline = pipelineDao.get(pl.getName());
                pipelineDao.delete(pipeline.getId());
            }
            catch (EmptyResultDataAccessException e) {
                // ignore
            }

            /**
             * Update the pipeline and validate the processors.
             */
            try {
                pipelineDao.create(new PipelineSpecV()
                        .setName(pl.getName())
                        .setDescription(pl.getDescription())
                        .setProcessors(pl.getProcessors().stream().map(
                                ref -> getProcessorRef(ref)).collect(Collectors.toList()))
                        .setType(PipelineType.valueOf(StringUtils.capitalize(pl.getType()))));
            } catch (EmptyResultDataAccessException e) {
                logger.warn("Failed to register pipeline: {}", pl);
            }
        }
    }

    @Override
    public void registerAllPlugins() {

        pluginInstaller.installAllPlugins(
                properties.getString("archivist.path.pluginSearchPath").split(":"));

        for (PluginSpec spec: pluginLoader.getPlugins()) {
            installPlugin(spec);
        }
    }

    public void registerProcessors(Plugin plugin, PluginSpec pspec) {
        if (pspec.getProcessors() == null || pspec.getProcessors().isEmpty()) {
            logger.warn("Plugin {} contains no processors", plugin);
            return;
        }
        for (ProcessorSpec spec: pspec.getProcessors()) {

            Processor proc;
            try {
                proc = processorDao.get(pspec.getName());
                processorDao.delete(proc.getId());
            }
            catch (EmptyResultDataAccessException e) {
                // ignore
            }

            processorDao.create(plugin, spec);
        }
    }

    /*
     * -------------------------------------------------------------------------------------------
     */

    @Override
    public PagedList<Plugin> getAllPlugins(Paging page) {
        return pluginDao.getAll(page);
    }

    @Override
    public List<Plugin> getAllPlugins() {
        return pluginDao.getAll();
    }

    @Override
    public Plugin getPlugin(String name) {
        return pluginDao.get(name);
    }

    @Override
    public Plugin getPlugin(int id) {
        return pluginDao.get(id);
    }

    @Override
    public List<Processor> getAllProcessors(Plugin plugin) {
        return processorDao.getAll(plugin);
    }

    @Override
    public Processor getProcessor(int id) {
        return processorDao.get(id);
    }

    private static final String BUILT_IN_PACKAGE =
            "com.zorroa.sdk.processor.builtin.";

    @Override
    public ProcessorRef getProcessorRef(String name, Map<String, Object> args) {
        if (name.startsWith(BUILT_IN_PACKAGE)) {
            return new SdkProcessorRef(name, args);
        }
        return processorDao.getRef(name).setArgs(args);
    }

    @Override
    public ProcessorRef getProcessorRef(String name) {
        if (name.startsWith(BUILT_IN_PACKAGE)) {
            return new SdkProcessorRef(name);
        }
        return processorDao.getRef(name).setArgs(Maps.newHashMap());
    }

    @Override
    public ProcessorRef getProcessorRef(ProcessorRef ref) {
        if (ref.getClassName().startsWith(BUILT_IN_PACKAGE)) {
            return new SdkProcessorRef(ref.getClassName(), ref.getArgs());
        }
        if (SdkProcessorRef.class.isAssignableFrom(ref.getClass())) {
            return ref;
        }
        return processorDao.getRef(ref.getClassName()).setArgs(ref.getArgs());
    }

    @Override
    public List<Processor> getAllProcessors(ProcessorFilter filter) {
        return processorDao.getAll(filter);
    }

    @Override
    public Processor getProcessor(String name) {
        return processorDao.get(name);
    }

    @Override
    public List<Processor> getAllProcessors() {
        return processorDao.getAll();
    }
}
