package com.zorroa.analyst.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;

/**
 * The Blender Ingestor looks for .blend files.
 */
public class BlenderIngestor extends IngestProcessor {

    public BlenderIngestor() {
        supportedFormats.add("blend");
    }

    @Override
    public void process(AssetBuilder assetBuilder) {
        /*
         * Tika can't seem to detect this, and Tika is not easy to add new detectors to
         * without recompiling it.
         */
        assetBuilder.getSource().setType("application/blender");
    }
}
