package com.zorroa.archivist.domain;

public class IngestProcessorWrapper {

    private String klass;
    private Object[] args;

    public String getKlass() {
        return klass;
    }
    public void setKlass(String klass) {
        this.klass = klass;
    }
    public Object[] getArgs() {
        return args;
    }
    public void setArgs(Object[] args) {
        this.args = args;
    }

}
