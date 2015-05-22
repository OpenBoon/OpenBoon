package com.zorroa.archivist.ingest;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.primitives.Ints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zorroa.archivist.IngestProxyException;
import com.zorroa.archivist.domain.AssetBuilder;
import com.zorroa.archivist.domain.Proxy;

public class ProxyProcessor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ProxyProcessor.class);

    public ProxyProcessor() { }

    @Override
    public void process(AssetBuilder builder, File file) {

        List<Proxy> result = Lists.newArrayList();
        Object scaleArgs = getArgs().get("proxy-scales");

        if (scaleArgs != null) {
            try {
                List<Double> scales = (List<Double>) scaleArgs;
                for (double scale: scales) {
                    try {
                        result.add(proxyService.makeProxy(file, scale));
                    } catch (IngestProxyException e) {
                        logger.error("Proxy creation failed: " + e, e);
                    }
                }
            } catch (ClassCastException e) {
                logger.error("Invalid argument format for 'scale', must be List<Double>");
            }
        }

        Object sizeArgs = getArgs().get("proxy-sizes");
        if (sizeArgs != null) {
            try {
                List<List<Integer>> sizes = (List<List<Integer>>) sizeArgs;
                for (List<Integer> size: sizes) {
                    try {
                        result.add(proxyService.makeProxy(file, size.get(0), size.get(1)));
                    } catch (IngestProxyException e) {
                        logger.error("Proxy creation failed: " + e, e);
                    }
                }
            } catch (ClassCastException e) {
                logger.error("Invalid argument format for 'size', must be List<List<Integer>>");
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
