package com.zorroa.analyst.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.processor.Argument;
import com.zorroa.archivist.sdk.schema.ImageSchema;
import org.elasticsearch.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AspectRatioIngestor  extends IngestProcessor {
    private static final Logger logger = LoggerFactory.getLogger(AspectRatioIngestor.class);

    @Argument
    String namespace = "aspect";

    static class Aspect {
        public float minAspect, maxAspect;
        public String keyword;
        public String field = "keywords";
        public boolean isKeyword = true;

        public Aspect(float minAspect, float maxAspect, String keyword) {
            this.minAspect = minAspect;
            this.maxAspect = maxAspect;
            this.keyword = keyword;
            this.field = field;
            this.isKeyword = isKeyword;
        }
    }

    @Argument
    private List<Aspect> aspects = defaultAspects;

    private static final List<Aspect> defaultAspects = ImmutableList.<Aspect>builder()
            .add(new Aspect(0f, 0.95f, "portrait"))
            .add(new Aspect(0.95f, 1.05f, "square"))
            .add(new Aspect(1.05f, 100000f, "landscape"))
            .build();

    @Override
    public void process(AssetBuilder asset) {

        if (!asset.isSuperType("image")) {
            return;
        }

        ImageSchema imageSchema = asset.getSchema("image", ImageSchema.class);

        final double aspect = (double) imageSchema.getWidth() / (double) imageSchema.getHeight();

        for (Aspect a : aspects) {
            if (aspect > a.minAspect && aspect < a.maxAspect) {
                asset.setAttr(namespace, a.field, a.keyword);
                if (a.isKeyword) {
                    asset.addKeywords(1, true, a.keyword);
                }
            }
        }
    }
}
