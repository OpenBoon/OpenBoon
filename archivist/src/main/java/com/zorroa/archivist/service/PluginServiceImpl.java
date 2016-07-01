package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.repository.ModuleDao;
import com.zorroa.common.repository.PluginDao;
import com.zorroa.sdk.config.ApplicationProperties;
import com.zorroa.sdk.exception.PluginException;
import com.zorroa.sdk.plugins.Module;
import com.zorroa.sdk.plugins.Plugin;
import com.zorroa.sdk.plugins.PluginLoader;
import com.zorroa.sdk.util.FileUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
public class PluginServiceImpl implements PluginService {

    private static final Logger logger = LoggerFactory.getLogger(PluginServiceImpl.class);

    @Autowired
    ApplicationProperties properties;

    @Autowired
    PluginDao pluginDao;

    @Autowired
    ModuleDao moduleDao;

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
        pluginLoader = new PluginLoader(pluginPath.toString());
        logger.info("Loading plugins from: {}", pluginPath);
    }

    @Override
    public Plugin registerPlugin(MultipartFile file) {
        synchronized(pluginLoader) {
            Path dst = pluginPath.resolve(file.getOriginalFilename());
            try {
                Files.copy(file.getInputStream(), dst);
                Path pluginPath = pluginLoader.expandPluginPackage(dst);
                Plugin plugin = pluginLoader.loadPlugin(pluginPath);
                registerPlugin(plugin);
                return plugin;

            } catch (Exception e) {
                throw new PluginException("Failed to install plugin, " + e.getMessage(), e);
            }
        }
    }

    @Override
    public Plugin registerPlugin(Path zipFilePath) {
        synchronized(pluginLoader) {
            if (zipFilePath.startsWith(pluginPath)) {
                throw new PluginException("Plugin '" + zipFilePath + "' is already installed.");
            }

            Path dst = pluginPath.resolve(FileUtils.filename(zipFilePath));
            try {
                Files.copy(zipFilePath, dst);
                Path pluginPath = pluginLoader.expandPluginPackage(dst);
                Plugin plugin = pluginLoader.loadPlugin(pluginPath);
                registerPlugin(plugin);
                return plugin;
            } catch (Exception e) {
                throw new PluginException("Failed to install plugin, " + e.getMessage(), e);
            }
        }
    }

    private void registerPlugin(Plugin p) {

        BulkRequestBuilder bulkRequest = client.prepareBulk();
        Map<String, Object> pluginDoc = getPluginRecord(p);

        bulkRequest.add(
                client.prepareUpdate(alias, "plugin", p.getName())
                        .setDoc(pluginDoc)
                        .setUpsert(pluginDoc));

        for (Module module: p.getModules()) {
            Map<String, Object> procDoc = getModuleRecord(p, module);
            String id = p.getName().concat(":").concat(module.getClassName());
            bulkRequest.add(
                    client.prepareUpdate(alias, "processor", id)
                            .setDoc(procDoc).setUpsert(procDoc));
        }

        BulkResponse bulk = bulkRequest.get();
        if (bulk.hasFailures()) {
            throw new PluginException("Unable to register plugin with database, " +
                    bulk.buildFailureMessage());
        }
    }

    @Override
    public PagedList<List<Plugin>> getPlugins(Paging page) {
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
    public Module getModule(String plugin, String name) {
        return moduleDao.get(plugin, name);
    }

    @Override
    public List<Module> getModules() {
        return moduleDao.getAll();
    }

    @Override
    public void registerAllPlugins() {
        pluginLoader.registerPlugins();

        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (Plugin p: pluginLoader.getPlugins()) {
            Map<String, Object> pluginDoc = getPluginRecord(p);
            bulkRequest.add(
                    client.prepareUpdate(alias, "plugin", p.getName())
                            .setDoc(pluginDoc)
                            .setUpsert(pluginDoc));

            for (Module module: p.getModules()) {
                Map<String, Object> procDoc = getModuleRecord(p, module);

                String id = p.getName().concat(":").concat(module.getClassName());
                bulkRequest.add(
                        client.prepareUpdate(alias, "module", id)
                                .setDoc(procDoc).setUpsert(procDoc));
            }
        }

        logger.info("Registering {} plugins", bulkRequest.numberOfActions());
        if (bulkRequest.numberOfActions() == 0) {
            return;
        }
        BulkResponse bulk = bulkRequest.get();
        if (bulk.hasFailures()) {
            throw new PluginException("Unable to register plugin with database, " +
                    bulk.buildFailureMessage());
        }
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
