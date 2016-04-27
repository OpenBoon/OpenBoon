package com.zorroa.analyst.service;


import com.zorroa.analyst.domain.PluginProperties;
import com.zorroa.archivist.sdk.domain.ApplicationProperties;
import com.zorroa.archivist.sdk.domain.Tuple;
import com.zorroa.archivist.sdk.plugins.Plugin;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.bootstrap.JarHell;
import org.elasticsearch.common.io.FileSystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Responsible for loading user supplied plugins.
 */
@Service
public class PluginServiceImpl implements PluginService {

    private static final Logger logger = LoggerFactory.getLogger(PluginServiceImpl.class);

    @Autowired
    ApplicationProperties properties;

    Path pluginsDirectory;

    List<Tuple<PluginProperties, Plugin>> loadedPlugins;

    URLClassLoader pluginClassLoader;

    @PostConstruct
    public void init() {
        pluginsDirectory = Paths.get(properties.getString("analyst.path.plugins"));
        // Bail out if no location exists for plugins
        if (!pluginsDirectory.toFile().exists()) {
            return;
        }

        try {
            List<PluginBundle> bundles = getPluginBundles(pluginsDirectory);
            List<Tuple<PluginProperties, Plugin>> loaded = loadBundles(bundles);
            loadedPlugins = Collections.unmodifiableList(loaded);

            for (Tuple<PluginProperties, Plugin> pair: loaded) {
                logger.info("processors {}", pair.getRight().getProcessors());
            }

        } catch (IOException ex) {
            throw new IllegalStateException("Unable to initialize plugins", ex);
        }
    }

    public IngestProcessor getIngestProcessor(String name) throws Exception {
        return Class.forName(name, false, pluginClassLoader).asSubclass(IngestProcessor.class).newInstance();
    }

    public List<PluginBundle> getPluginBundles(Path pluginsDirectory) throws IOException {

        List<PluginBundle> bundles = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsDirectory)) {

            for (Path plugin : stream) {
                if (FileSystemUtils.isHidden(plugin)) {
                    logger.debug("--- skip hidden plugin file[{}]", plugin.toAbsolutePath());
                    continue;
                }

                logger.debug("--- adding plugin [{}]", plugin.toAbsolutePath());
                final PluginProperties pluginProps;
                try {
                    pluginProps = PluginProperties.readFromProperties(plugin);
                } catch (IOException e) {
                    throw new IllegalStateException("Could not load plugin descriptor for existing plugin ["
                            + plugin.getFileName() + "]. Was the plugin built before 2.0?", e);
                }

                List<URL> urls = new ArrayList<>();
                try (DirectoryStream<Path> jarStream = Files.newDirectoryStream(plugin, "*.jar")) {
                    for (Path jar : jarStream) {
                        JarFile file = new JarFile(jar.toFile());
                        Enumeration<JarEntry> entries = file.entries();

                        while(entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            if (entry.getName().matches(".*\\.dylib$")) {
                                logger.info("{}", entry.getName());
                            }
                        }

                        // normalize with toRealPath to get symlinks out of our hair
                        urls.add(jar.toRealPath().toUri().toURL());
                    }
                }

                PluginBundle bundle = new PluginBundle();
                bundles.add(bundle);
                bundle.plugins.add(pluginProps);
                bundle.urls.addAll(urls);
            }
        }

        return bundles;
    }

    private List<Tuple<PluginProperties,Plugin>> loadBundles(List<PluginBundle> bundles) {
        List<Tuple<PluginProperties, Plugin>> plugins = new ArrayList<>();
        final List<URL> allJars = new ArrayList<>();

        for (PluginBundle bundle : bundles) {
            // jar-hell check the bundle against the parent classloader
            // pluginmanager does it, but we do it again, in case lusers mess with jar files manually
            try {
                final List<URL> jars = new ArrayList<>();
                jars.addAll(Arrays.asList(JarHell.parseClassPath()));
                jars.addAll(bundle.urls);
                allJars.addAll(bundle.urls);
            } catch (Exception e) {
                throw new IllegalStateException("failed to load bundle " + bundle.urls + " due to jar hell", e);
            }
        }

        pluginClassLoader = URLClassLoader.newInstance(allJars.toArray(new URL[]{}), getClass().getClassLoader());

        for (PluginBundle bundle : bundles) {
            for (PluginProperties pluginProps : bundle.plugins) {
                final Class<? extends Plugin> pluginClass = loadPluginClass(pluginProps.getClassName(), pluginClassLoader);
                final Plugin plugin = loadPlugin(pluginClass);
                plugins.add(new Tuple<>(pluginProps, plugin));
            }
        }

        return Collections.unmodifiableList(plugins);
    }

    private Class<? extends Plugin> loadPluginClass(String className, ClassLoader loader) {
        try {
            return loader.loadClass(className).asSubclass(Plugin.class);
        } catch (ClassNotFoundException e) {
            throw new ElasticsearchException("Could not find plugin class [" + className + "]", e);
        }
    }

    private Plugin loadPlugin(Class<? extends Plugin> pluginClass) {
        try {
            try {
                return pluginClass.getConstructor(ApplicationProperties.class).newInstance(properties);
            } catch (NoSuchMethodException e) {
                try {
                    return pluginClass.getConstructor().newInstance();
                } catch (NoSuchMethodException e1) {
                    throw new ElasticsearchException("No constructor for [" + pluginClass + "]. A plugin class must " +
                            "have either an empty default constructor or a single argument constructor accepting a " +
                            "Settings instance");
                }
            }
        } catch (Throwable e) {
            throw new ElasticsearchException("Failed to load plugin class [" + pluginClass.getName() + "]", e);
        }
    }

    public List<Tuple<PluginProperties, Plugin>> getLoadedPlugins() {
        return loadedPlugins;
    }

    /**
     * A bundle supports multiple plugins in the same plugin directory.
     */
    private static class PluginBundle {
        List<PluginProperties> plugins = new ArrayList<>();
        List<URL> urls = new ArrayList<>();
    }
}
