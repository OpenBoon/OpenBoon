package com.zorroa.archivist.ingest;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.primitives.Ints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zorroa.archivist.domain.AssetBuilder;
import com.zorroa.archivist.domain.Proxy;
import com.zorroa.archivist.domain.ProxyOutput;

public class ProxyProcessor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ProxyProcessor.class);

    public ProxyProcessor() { }

    @Override
    public void process(AssetBuilder builder, File file) {
        List<Proxy> result = Lists.newArrayList();
        for (ProxyOutput output: proxyConfig.getOutputs()) {
            try {
                result.add(proxyService.makeProxy(file, output));
            } catch (IOException e) {
                logger.warn("Failed to create proxy {}, ", output, e);
            }
        }

        /*
         * Sort by # of pixels?
         */
        Collections.sort(result, new Comparator<Proxy>() {
            @Override
            public int compare(Proxy o1, Proxy o2) {
                return Ints.compare(o1.getWidth() * o1.getHeight(), o2.getWidth() * o2.getHeight());
            }
        });

        builder.document.put("proxies", result);
    }
}
