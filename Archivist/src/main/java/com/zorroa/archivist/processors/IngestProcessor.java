package com.zorroa.archivist.processors;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.zorroa.archivist.domain.AssetBuilder;
import com.zorroa.archivist.domain.ProxyConfig;
import com.zorroa.archivist.service.ImageService;

public abstract class IngestProcessor {

    @Autowired
    protected ImageService imageService;

    protected ProxyConfig proxyConfig;

    public abstract void process(AssetBuilder asset);

    private Map<String, Object> args;

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }

    public void setProxyConfig(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }
}
