package com.zorroa.ingestors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.ProxySchema;
import com.zorroa.archivist.sdk.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AspectRatioIngestor  extends IngestProcessor {
    private static final Logger logger = LoggerFactory.getLogger(AspectRatioIngestor.class);

    @Override
    public void process(AssetBuilder asset) {

        if (!asset.isSuperType("image")) {
            return;
        }

        List<AspectRatioOptions> aspects = Json.Mapper.convertValue(getArgs().get("aspects"),
                new TypeReference<List<AspectRatioOptions>>() {});

        if (aspects == null) {
            aspects = Lists.newArrayList(
                    new AspectRatioOptions(0.0f, 0.95f, "portrait"),
                    new AspectRatioOptions(0.95f, 1.05f, "square"),
                    new AspectRatioOptions(1.05f, 1000.0f, "landscape")
            );
        }

        ProxySchema proxyList = asset.getSchema("proxies", ProxySchema.class);
        double aspect;
        int width;
        int height;


        // If the image processor hasn't run (is this even possible?), the "image.width" and "image.height" attributes
        // are not there and getAttr returns null. This happens during unit tests, it seems.
        // So we check, and if not there, we take width and height from the first proxy.
        try {
            width = asset.getAttr("image", "width");
            height = asset.getAttr("image", "height");
        } catch (Exception e) {
            if (proxyList == null) {
                logger.warn("Cannot find proxy list for {}, skipping Aspect ratio analysis.", asset);
                return;
            }
            width = proxyList.get(0).getWidth();
            height = proxyList.get(0).getHeight();
        }

        aspect = (double) width / (double) height;

        for (AspectRatioOptions line : aspects) {
            if (line.isWithin(aspect)) {
                asset.addKeywords(1, true, line.getKeyword());
                asset.setAttr("aspectratio", "keywords", line.getKeyword());
            }
        }
    }
}
