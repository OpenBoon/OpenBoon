package com.zorroa.archivist.processors;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.zorroa.archivist.domain.AssetBuilder;
import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestPipeline;
import com.zorroa.archivist.domain.ProxyConfig;
import com.zorroa.archivist.service.ImageService;

public abstract class IngestProcessor {

    @Autowired
    protected ImageService imageService;

    protected ProxyConfig proxyConfig;

    protected IngestPipeline ingestPipeline;

    protected Ingest ingest;

    public abstract void process(AssetBuilder asset);

    private Map<String, Object> args;

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }

    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    public IngestPipeline getIngestPipeline() {
        return ingestPipeline;
    }

    public void setIngestPipeline(IngestPipeline ingestPipeline) {
        this.ingestPipeline = ingestPipeline;
    }

    public Ingest getIngest() {
        return ingest;
    }

    public void setIngest(Ingest ingest) {
        this.ingest = ingest;
    }
}
