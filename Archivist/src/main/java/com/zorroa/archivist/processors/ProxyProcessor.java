package com.zorroa.archivist.processors;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.primitives.Ints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.zorroa.archivist.domain.AssetBuilder;
import com.zorroa.archivist.domain.Proxy;
import com.zorroa.archivist.domain.ProxyOutput;
import com.zorroa.archivist.service.ImageService;

public class ProxyProcessor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ProxyProcessor.class);

    @Autowired
    protected ImageService imageService;

    public ProxyProcessor() { }

    @Override
    public void process(AssetBuilder asset) {


        List<Proxy> result = Lists.newArrayList();
        for (ProxyOutput output: getProxyConfig().getOutputs()) {
            try {
                result.add(imageService.makeProxy(asset.getFile(), output));
            } catch (IOException e) {
                logger.warn("Failed to create proxy {}, ", output, e);
            }
        }


        Collections.sort(result, new Comparator<Proxy>() {
            @Override
            public int compare(Proxy o1, Proxy o2) {
                return Ints.compare(o1.getWidth() * o1.getHeight(), o2.getWidth() * o2.getHeight());
            }
        });

        asset.document.put("proxies", result);

    }
}
