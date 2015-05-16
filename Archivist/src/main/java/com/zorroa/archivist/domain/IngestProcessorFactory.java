package com.zorroa.archivist.domain;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.google.common.collect.Maps;
import com.zorroa.archivist.ingest.IngestProcessor;

public class IngestProcessorFactory {

    private final ClassLoader classLoader = IngestProcessorFactory.class.getClassLoader();

    private String klass;
    private Map<String, Object> args;
    private IngestProcessor processor = null;

    public IngestProcessorFactory() { }

    public void init() {
        if (processor == null) {
            try {
                Class<?> pclass = classLoader.loadClass(klass);
                processor = (IngestProcessor) pclass.getConstructor().newInstance();
                processor.setArgs(args);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {

            }
        }
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

    public IngestProcessor get() {
        return processor;
    }
}
