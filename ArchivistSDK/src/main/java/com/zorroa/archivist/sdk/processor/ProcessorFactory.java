package com.zorroa.archivist.sdk.processor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import com.zorroa.archivist.sdk.domain.ArchivistException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

/**
 * Created by chambers on 11/1/15.
 */
public class ProcessorFactory<T extends Processor> implements Serializable {

    static final long serialVersionUID = 1;

    private static final Logger logger = LoggerFactory.getLogger(ProcessorFactory.class);

    private String klass;
    private Map<String, Object> args = Maps.newHashMap();

    /**
     * These is transient because if we serialize this class, we don't want to serialize the instance
     * of the processor or classloader.  Its volatile because the instance is created in the main thread and
     * then used by others.  The other threads might not see its been created.
     */
    private transient volatile T instance = null;

    /**
     * The clasLoader is static so that a shared instance is used to load plugins, which may contain
     * native code. Multiple loaders would result in UnsatisfiedLinkErrors when multiple attempts are
     * made to dynamically load dependent jnilibs.
     */
    private static ClassLoader classLoader = null;

    public ProcessorFactory() { }

    public ProcessorFactory(String klass) {
        this.klass = klass;
    }

    public ProcessorFactory(String klass, Map<String, Object> args) {
        this.klass = klass;
        this.args.putAll(args);
    }

    public ProcessorFactory(Class<?> klass) {
        this.klass = klass.getCanonicalName();
    }

    public String getKlass() {
        return klass;
    }

    public String getKlassName() {
        return klass.substring(klass.lastIndexOf('.')+1);
    }
    public void setKlass(String klass) {
        this.klass = klass;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public Object getArg(String name, Object dvalue) {
        return args.getOrDefault(name, dvalue);
    }

    public Object getArg(String name) {
        return args.get(name);
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }

    public void init() {

        /*
         * Check if its already been initialized.
         */
        if (instance != null) {
            return;
        }

        if (classLoader == null) {
            classLoader = getSiteClassLoader();
        }

        try {
            Class<?> pclass = classLoader.loadClass(klass);
            instance = (T) pclass.getConstructor().newInstance();
            instance.setArgs(args);
        } catch (Exception e) {
            throw new ArchivistException("Failed to load class '" + klass + "', " + e, e);
        }
    }

    @JsonIgnore
    public T getInstance() {
        return instance;
    }

    @JsonIgnore
    public T newInstance() {
        if (classLoader == null) {
            classLoader = getSiteClassLoader();
        }

        try {
            Class<?> pclass = classLoader.loadClass(klass);
            instance = (T) pclass.getConstructor().newInstance();
            instance.setArgs(args);
            return instance;
        } catch (Exception e) {
            throw new ArchivistException("Failed to load class '" + klass + "', " + e, e);
        }
    }

    @JsonIgnore
    public ClassLoader getSiteClassLoader() {
        // Create an array of URLs to search as a classpath when loading processors

        String sitePath = System.getenv("ZORROA_SITE_PATH");
        if (sitePath == null) {
            logger.warn("ZORROA_SITE_PATH is not set.");
            return ProcessorFactory.class.getClassLoader();
        }

        File folder = new File(sitePath);
        if (!folder.exists()) {
            logger.warn("Invalid ZORROA_SITE_PATH: " + sitePath);
            return ProcessorFactory.class.getClassLoader();
        }

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
                } catch (StringIndexOutOfBoundsException e) {
                }
            }
        }

        URL[] zorroaJarURLs = urls.toArray(new URL[urls.size()]);
        return new URLClassLoader(zorroaJarURLs, ProcessorFactory.class.getClassLoader());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("klass", klass)
                .add("args", args)
                .toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }

        try {
            ProcessorFactory other = (ProcessorFactory) object;

            if (!Objects.equals(klass, other.getKlass())) {
                return false;
            }
            if (!Maps.difference(args, other.getArgs()).areEqual()) {
                return false;
            }

            return true;
        } catch(ClassCastException e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(klass, args);
    }

}
