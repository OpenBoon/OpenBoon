package com.zorroa.archivist.domain;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.zorroa.archivist.sdk.IngestProcessor;

public class IngestProcessorFactory implements Serializable {

    private static final long serialVersionUID = 945863554465836155L;

    private static final Logger logger = LoggerFactory.getLogger(InvocationTargetException.class);

    private static final ClassLoader classLoader = IngestProcessorFactory.class.getClassLoader();

    private String klass;
    private Map<String, Object> args;

    private transient IngestProcessor processor = null;

    public IngestProcessorFactory() { }

    public IngestProcessorFactory(Class<?> klass) {
        this.klass = klass.getCanonicalName();
        this.args = Maps.newHashMap();
    }

    public IngestProcessor init() {
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
