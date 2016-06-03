package com.zorroa.analyst;

import com.zorroa.sdk.domain.AssetBuilder;
import com.zorroa.sdk.processor.IngestProcessor;

/**
 * Created by chambers on 4/28/16.
 */
public class UnitTestIngestor extends IngestProcessor {

    public UnitTestIngestor() {
        supportedFormats.add("jpg");
    }

    @Override
    public void process(AssetBuilder assetBuilder) {
        Integer value = assetBuilder.getAttr("unittest.value");
        if (value == null) {
            assetBuilder.setAttr("unittest.value", 1);
        }
        else {
            assetBuilder.setAttr("unittest.value", value.intValue() + 1);
        }

    }
}
