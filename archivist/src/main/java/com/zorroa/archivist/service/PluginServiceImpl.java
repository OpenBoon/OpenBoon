package com.zorroa.archivist.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.PipelineDao;
import com.zorroa.archivist.repository.PluginDao;
import com.zorroa.archivist.repository.ProcessorDao;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.exception.PluginException;
import com.zorroa.sdk.plugins.PipelineSpec;
import com.zorroa.sdk.plugins.PluginRegistry;
import com.zorroa.sdk.plugins.PluginSpec;
import com.zorroa.sdk.plugins.ProcessorSpec;
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

    PluginRegistry pluginRegistry;

    Path pluginPath;
    Path modelPath;

    @PostConstruct
    public void init() {
        String _path = properties.getString("archivist.path.plugins");

        pluginPath =  FileUtils.normalize(Paths.get(_path));
        modelPath = FileUtils.normalize(Paths.get(modelPathStr));

        logger.info("Loading plugins from: {}", pluginPath);
        if (pluginPath.toFile().mkdirs()) {
            logger.info("Plugin path did not exist: {}", pluginPath);
        }

        /**
         * Create the registry, but ArchivistRepositorySetup loads the plugins
         * once the application is fully initialized.
         */
        pluginRegistry = new PluginRegistry(sharedData);
    }

    @Override
    public Plugin installPlugin(MultipartFile file) {
        synchronized(pluginRegistry) {
            try {
                Path pluginPath = pluginRegistry.unpackPluginPackage(file.getInputStream());
                PluginSpec plugin = pluginRegistry.loadPlugin(pluginPath);
                createPluginRecord(plugin);
                return pluginDao.get(plugin.getName());

            } catch (Exception e) {
                throw new PluginException("Failed to install plugin, " + e.getMessage(), e);
            }
        }
    }

    @Override
    public Plugin installPlugin(Path zipFilePath) {
        synchronized(pluginRegistry) {
            if (zipFilePath.startsWith(pluginPath)) {
                throw new PluginException("Plugin '" + zipFilePath + "' is already installed.");
            }

            try {
                Path pluginPath = pluginRegistry.unpackPluginPackage(zipFilePath);
                PluginSpec plugin = pluginRegistry.loadPlugin(pluginPath);
                createPluginRecord(plugin);
                return pluginDao.get(plugin.getName());
            } catch (Exception e) {
                throw new PluginException("Failed to install plugin, " + e.getMessage(), e);
            }
        }
    }

    private void createPluginRecord(PluginSpec spec) {
        boolean newOrChanged = false;
        Plugin plugin;
        try {
            plugin = pluginDao.get(spec.getName());
            if (!spec.getMd5().equals(plugin.getMd5())) {
                logger.info("The plugin {} has changed, reloading.", plugin.getName());
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
    public void installAndRegisterAllPlugins() {

        pluginRegistry.installAllPlugins(
                properties.split("archivist.path.pluginSearchPath", ":"));
        pluginRegistry.loadInstalledPlugins();

        for (PluginSpec spec: pluginRegistry.getPlugins()) {
            createPluginRecord(spec);
        }
    }

    public void registerProcessors(Plugin plugin, PluginSpec pspec) {
        if (pspec.getProcessors() == null || pspec.getProcessors().isEmpty()) {
            logger.warn("Plugin {} contains no processors", plugin);
            return;
        }
        processorDao.deleteAll(plugin);

        for (ProcessorSpec spec: pspec.getProcessors()) {

            Processor proc;
            try {
                proc = processorDao.get(spec.getClassName());
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
    public PagedList<Plugin> getAllPlugins(Pager page) {
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
    public boolean deletePlugin(Plugin plugin) {
        return pluginDao.delete(plugin.getId());
    }

    @Override
    public List<Processor> getAllProcessors(Plugin plugin) {
        return processorDao.getAll(plugin);
    }

    @Override
    public Processor getProcessor(int id) {
        return processorDao.get(id);
    }

    @Override
    public ProcessorRef getProcessorRef(String name, Map<String, Object> args) {
        return processorDao.getRef(name).setArgs(args);
    }

    @Override
    public ProcessorRef getProcessorRef(String name) {
        return processorDao.getRef(name).setArgs(Maps.newHashMap());
    }

    @Override
    public ProcessorRef getProcessorRef(ProcessorRef ref) {
        return processorDao.getRef(ref.getClassName()).setArgs(ref.getArgs());
    }

    @Override
    public List<ProcessorRef> getProcessorRefs(List<ProcessorRef> refs) {
        if (refs == null) {
            return null;
        }
        List<ProcessorRef> result = Lists.newArrayListWithCapacity(refs.size());
        for (ProcessorRef ref: refs) {
            result.add(processorDao.getRef(ref.getClassName()).setArgs(ref.getArgs()));
        }
        return result;
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
