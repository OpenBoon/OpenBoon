package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.domain.PipelineSpec;
import com.zorroa.archivist.domain.PipelineType;
import com.zorroa.archivist.repository.PipelineDao;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.repository.ModuleDao;
import com.zorroa.common.repository.PluginDao;
import com.zorroa.sdk.config.ApplicationProperties;
import com.zorroa.sdk.exception.PluginException;
import com.zorroa.sdk.plugins.Module;
import com.zorroa.sdk.plugins.Pipeline;
import com.zorroa.sdk.plugins.Plugin;
import com.zorroa.sdk.plugins.PluginLoader;
import com.zorroa.sdk.util.FileUtils;
import com.zorroa.sdk.util.StringUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Responsible for installing and registering plugins with elastic.
 */
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class PluginServiceImpl implements PluginService {

    private static final Logger logger = LoggerFactory.getLogger(PluginServiceImpl.class);

    @Autowired
    ApplicationProperties properties;

    @Autowired
    PluginDao pluginDao;

    @Autowired
    ModuleDao moduleDao;

    @Autowired
    PipelineDao pipelineDao;

    @Autowired
    Client client;

    @Value("${zorroa.cluster.index.alias}")
    private String alias;

    PluginLoader pluginLoader;

    Path pluginPath;

    @PostConstruct
    public void init() {
        String _path = properties.getString("archivist.path.plugins");
        if (_path.contains(":")) {
            throw new RuntimeException("Cannot specify multiple plugin paths.");
        }
        pluginPath = Paths.get(_path).toAbsolutePath().normalize();
        logger.info("Loading plugins from: {}", pluginPath);
        if (pluginPath.toFile().mkdirs()) {
            logger.info("Plugin path did not exist: {}", pluginPath);
        }
        pluginLoader = new PluginLoader(pluginPath.toString());
    }

    @Override
    public Plugin installPlugin(MultipartFile file) {
        synchronized(pluginLoader) {
            Path dst = pluginPath.resolve(file.getOriginalFilename());
            try {
                Files.copy(file.getInputStream(), dst);
                Path pluginPath = pluginLoader.unpackPluginPackage(dst);
                Plugin plugin = pluginLoader.loadPlugin(pluginPath);
                installPlugin(plugin);
                return plugin;

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
                Path pluginPath = pluginLoader.unpackPluginPackage(dst);
                Plugin plugin = pluginLoader.loadPlugin(pluginPath);
                installPlugin(plugin);
                return plugin;
            } catch (Exception e) {
                throw new PluginException("Failed to install plugin, " + e.getMessage(), e);
            }
        }
    }

    private void installPlugin(Plugin p) {

        BulkRequestBuilder bulkRequest = client.prepareBulk();
        Map<String, Object> pluginDoc = getPluginRecord(p);

        bulkRequest.add(
                client.prepareUpdate(alias, "plugin", p.getName())
                        .setDoc(pluginDoc)
                        .setUpsert(pluginDoc));

        for (Module module: p.getModules()) {
            Map<String, Object> procDoc = getModuleRecord(p, module);
            String id = moduleId(module.getType(), p.getName(), module.getName());
            bulkRequest.add(
                    client.prepareUpdate(alias, "module", id)
                            .setDoc(procDoc).setUpsert(procDoc));
        }

        BulkResponse bulk = bulkRequest.get();
        if (bulk.hasFailures()) {
            throw new PluginException("Unable to register plugin with database, " +
                    bulk.buildFailureMessage());
        }

        // Register other data.
        registerPipelines(p);
    }

    public void registerPipelines(Plugin p) {
        if (p.getPipelines() == null) {
            return;
        }
        for (Pipeline pl: p.getPipelines()) {
            try {
                pipelineDao.create(new PipelineSpec()
                        .setName(pl.getName())
                        .setDescription(pl.getDescription())
                        .setProcessors(pl.getProcessors())
                        .setType(PipelineType.valueOf(StringUtils.capitalize(pl.getType()))));
                logger.info("registering pipeline: {}", pl.getName());
            } catch (DuplicateKeyException e) {
                // catch the duplicates
            }
        }
    }

    @Override
    public PagedList<Plugin> getPlugins(Paging page) {
        return pluginDao.getAll(page);
    }

    @Override
    public List<Plugin> getPlugins() {
        return pluginDao.getAll();
    }

    @Override
    public Plugin get(String name) {
        return pluginDao.get(name);
    }

    @Override
    public List<Module> getModules(String plugin) {
        return moduleDao.getAll(plugin);
    }

    @Override
    public List<Module> getModules(String plugin, String type) {
        return moduleDao.getAll(plugin, type);
    }

    @Override
    public Module getModule(String id) {
        return moduleDao.get(id);
    }
    @Override
    public List<Module> getModules() {
        return moduleDao.getAll();
    }

    @Override
    public void registerAllPlugins() {
        pluginLoader.unpackAndLoadAllPlugins();

        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (Plugin p: pluginLoader.getPlugins()) {
            Map<String, Object> pluginDoc = getPluginRecord(p);
            bulkRequest.add(
                    client.prepareUpdate(alias, "plugin", p.getName())
                            .setDoc(pluginDoc)
                            .setUpsert(pluginDoc));

            for (Module module: p.getModules()) {
                Map<String, Object> procDoc = getModuleRecord(p, module);
                String id = moduleId(module.getType(), p.getName(), module.getName());
                bulkRequest.add(
                        client.prepareUpdate(alias, "module", id)
                                .setDoc(procDoc).setUpsert(procDoc));
            }

            registerPipelines(p);
        }

        logger.info("Registering {} plugin modules", bulkRequest.numberOfActions());
        if (bulkRequest.numberOfActions() == 0) {
            return;
        }
        BulkResponse bulk = bulkRequest.get();
        if (bulk.hasFailures()) {
            throw new PluginException("Unable to register plugin with database, " +
                    bulk.buildFailureMessage());
        }
    }

    private static String moduleId(String t, String p, String n) {
        return String.join(":", t, p, n).replace(" ", "");
    }

    private Map<String, Object> getModuleRecord(Plugin p, Module module) {
        return ImmutableMap.<String,Object>builder()
                .put("plugin", p.getName())
                .put("language", p.getLanguage())
                .put("version",p.getVersion())
                .put("className", module.getClassName())
                .put("name", module.getName())
                .put("type", module.getType())
                .put("display", module.getDisplay()).build();
    }

    private Map<String, Object> getPluginRecord(Plugin p) {
        return ImmutableMap.of(
                "name", p.getName(),
                "language", p.getLanguage(),
                "version",p.getVersion(),
                "description",p.getDescription(),
                "publisher",p.getPublisher());
    }
}
