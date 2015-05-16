package com.zorroa.archivist.domain;

import java.util.List;

public class IngestProcessorFactory {

    private String klass;
    private List<Object> args;

    public IngestProcessorFactory() { }

    public IngestProcessorFactory(String klass,  List<Object> args) {
        this.klass = klass;
        this.args = args;
    }

    public String getKlass() {
        return klass;
    }
    public void setKlass(String klass) {
        this.klass = klass;
    }
    public List<Object> ßgetArgs() {
        return args;
    }
    public void setArgs(List<Object> args) {
        this.args = args;
    }

}
