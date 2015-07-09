package com.zorroa.archivist.domain;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Map;

import com.zorroa.archivist.sdk.AssetBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.zorroa.archivist.sdk.IngestProcessor;

public class IngestProcessorFactory implements Serializable {

    private static final long serialVersionUID = 945863554465836155L;

    private static final Logger logger = LoggerFactory.getLogger(InvocationTargetException.class);

    private String klass;
    private Map<String, Object> args;

    private transient IngestProcessor processor = null;

    public IngestProcessorFactory() { }

    public IngestProcessorFactory(Class<?> klass) {
        this.klass = klass.getCanonicalName();
        this.args = Maps.newHashMap();
    }

    private static ClassLoader classLoader = null;

    public ClassLoader getSiteClassLoader() {
        if (classLoader == null) {
            classLoader = AssetBuilder.class.getClassLoader();
            // Create an array of URLs to search as a classpath when loading processors
            URL[] zorroaJarURLs = null;
            Map<String, String> env = System.getenv();
            String sitePath = env.get("ZORROA_SITE_PATH");
            if (sitePath == null) {
                logger.warn("ZORROA_SITE_PATH is not set.");
                classLoader = AssetBuilder.class.getClassLoader();
            } else {
                File folder = new File(sitePath);
                if (!folder.exists()) {
                    logger.warn("Invalid ZORROA_SITE_PATH: " + sitePath);
                    classLoader = AssetBuilder.class.getClassLoader();
                } else {
                    File[] listOfFiles = folder.listFiles();
                    ArrayList<URL> urls = new ArrayList<URL>();
                    for (int i = 0; i < listOfFiles.length; i++) {
                        if (listOfFiles[i].isFile()) {
                            String path = listOfFiles[i].getAbsolutePath();
                            try {
                                String ext = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
                                if (ext.equals("jar")) {
                                    try {
                                        URL url = new File(path).toURI().toURL();
                                        urls.add(url);
                                    } catch (IOException e) {
                                        logger.error("Cannot create URL to Zorroa jar file " + path);
                                        e.printStackTrace();
                                    }
                                }
                            } catch (java.lang.StringIndexOutOfBoundsException e) {
                            }
                        }
                    }
                    zorroaJarURLs = urls.toArray(new URL[urls.size()]);
                    classLoader = new URLClassLoader(zorroaJarURLs, AssetBuilder.class.getClassLoader());
                }
            }
        }
        return classLoader;
    }

    public IngestProcessor init() {
        if (classLoader == null) {
            classLoader = getSiteClassLoader();
        }
        if (processor == null) {
            try {
                Class<?> pclass = classLoader.loadClass(klass);
                processor = (IngestProcessor) pclass.getConstructor().newInstance();
                processor.setArgs(args);
                return processor;
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                logger.warn("Failed to initialize ingest processor {} with args {}", klass, args);
                return null;
            }
        }

        return processor;
    }

    public IngestProcessorFactory(String klass) {
        this.klass = klass;
        this.args = Maps.newHashMap();
    }

    public IngestProcessorFactory(String klass,  Map<String, Object> args) {
        this.klass = klass;
        this.args = args;
    }

    public String getKlass() {
        return klass;
    }

    public void setKlass(String klass) {
        this.klass = klass;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }

    public IngestProcessor getProcessor() {
        return processor;
    }

    public void setProcessor(IngestProcessor processor) {
        this.processor = processor;
    }
}
