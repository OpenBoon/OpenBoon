package com.zorroa.archivist.processors;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.zorroa.archivist.domain.AssetBuilder;
import com.zorroa.archivist.domain.ProxyOutput;
import com.zorroa.archivist.service.ImageService;

public abstract class IngestProcessor {

    @Autowired
    protected ImageService imageService;

    protected List<ProxyOutput> proxyOutputs;

    public abstract void process(AssetBuilder asset);

    private Map<String, Object> args;

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }

    public List<ProxyOutput> getProxyOutputs() {
        return proxyOutputs;
    }

    public void setProxyOutputs(List<ProxyOutput> proxyOutputs) {
        this.proxyOutputs = proxyOutputs;
    }
}
