package com.zorroa.analyst.service;


import com.google.common.collect.ImmutableList;
import com.zorroa.analyst.domain.PluginProperties;
import com.zorroa.sdk.config.ApplicationProperties;
import com.zorroa.sdk.domain.Tuple;
import com.zorroa.sdk.filesystem.ObjectFileSystem;
import com.zorroa.sdk.plugins.Plugin;
import com.zorroa.sdk.processor.ingest.IngestProcessor;
import com.zorroa.sdk.util.FileUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.bootstrap.JarHell;
import org.elasticsearch.common.io.FileSystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Responsible for loading user supplied plugins.
 */
@Service
public class PluginServiceImpl implements PluginService {

    private static final Logger logger = LoggerFactory.getLogger(PluginServiceImpl.class);

    @Autowired
    ApplicationProperties properties;

    @Autowired
    ObjectFileSystem fileSystem;

    AtomicBoolean pluginsLoaded = new AtomicBoolean(false);

    Path pluginsDirectory;

    List<Tuple<PluginProperties, Plugin>> loadedPlugins = ImmutableList.of();

    ClassLoader pluginClassLoader;

    @PostConstruct
    public void init() {

        /**
         * Initially set class loader to our own class loader.  This will allow us
         * to load unit test processors.  If the plugin system is active, a new
         * pluginClassLoader is created for loading classes from the plugin jars.
         */
        pluginClassLoader = getClass().getClassLoader();

        if (!properties.getBoolean("analyst.plugins.enabled")) {
            logger.info("Plugins have been disabled via analyst configuration.");
            return;
        }

        loadPlugins();
    }

    @Override
    public void loadPlugins() {

        if (!pluginsLoaded.compareAndSet(false, true)) {
            logger.warn("PluginService.loadPlugins called but plugins are already loaded.");
            return;
        }

        pluginsDirectory = Paths.get(properties.getString("analyst.path.plugins"));
        // Bail out if no location exists for plugins
        if (!pluginsDirectory.toFile().exists()) {
            logger.warn("Plugin directory does not exist: {}", pluginsDirectory);
            return;
        }

        try {
            expandPluginBundles(pluginsDirectory);
            List<PluginBundle> bundles = getPluginBundles(pluginsDirectory);
            List<Tuple<PluginProperties, Plugin>> loaded = loadBundles(bundles);
            loadedPlugins = Collections.unmodifiableList(loaded);

            for (Tuple<PluginProperties, Plugin> pair: loaded) {
                logger.info("processors {}", pair.getRight().getIngestProcessors());
            }

        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load plugins", ex);
        }
    }

    @Override
    public IngestProcessor getIngestProcessor(String name) throws Exception {
        IngestProcessor result = Class.forName(name, false, pluginClassLoader)
                .asSubclass(IngestProcessor.class).newInstance();
        result.setObjectFileSystem(fileSystem);
        result.setApplicationProperties(properties);
        return result;
    }

    /**
     * Check the pluginsDirectory for plugin zip files and decompress them.  If the directory
     * for the plugin alrady exists, skip over it. This decision might be reversed later on
     * and we'll allow overwriting of plugins.
     *
     * @param pluginsDirectory
     * @throws IOException
     */
    private void expandPluginBundles(Path pluginsDirectory) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsDirectory)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    continue;
                }
                // Note: plugins have to end with -plugin.zip
                if (!path.toString().endsWith("-plugin.zip")) {
                    continue;
                }

                String basename = FileUtils.basename(path.toFile().getName()).replace("-plugin", "");
                File folder = new File(path.getParent().toString() +"/" + basename);

                if (folder.exists()) {
                    logger.warn("Skipping plugin install, file already exists: {}", basename);
                    continue;
                }

                logger.info("Expanding plugin bundle: {}", path);
                unzip(path.toString(), pluginsDirectory);
            }
        }

    }

    public void unzip(String zipFilePath, Path destDir) throws IOException {
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        while (entry != null) {
            String filePath = destDir.toString() + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                extractFile(zipIn, filePath);
            } else {
                File dir = new File(filePath);
                dir.mkdir();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }

    private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[4096];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    private List<PluginBundle> getPluginBundles(Path pluginsDirectory) throws IOException {
        List<PluginBundle> bundles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsDirectory)) {
            for (Path plugin : stream) {
                if (FileSystemUtils.isHidden(plugin)) {
                    logger.debug("--- skip hidden plugin file[{}]", plugin.toAbsolutePath());
                    continue;
                }

                if (!Files.isDirectory(plugin)) {
                    continue;
                }

                logger.debug("loading plugin [{}]", plugin.toAbsolutePath());
                final PluginProperties pluginProps;
                try {
                    pluginProps = PluginProperties.readFromProperties(plugin);
                } catch (IOException e) {
                    throw new IllegalStateException("Could not load plugin descriptor for existing plugin ["
                            + plugin.getFileName() + "]", e);
                }

                List<URL> urls = new ArrayList<>();
                try (DirectoryStream<Path> jarStream = Files.newDirectoryStream(plugin, "*.jar")) {
                    for (Path jar : jarStream) {
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
