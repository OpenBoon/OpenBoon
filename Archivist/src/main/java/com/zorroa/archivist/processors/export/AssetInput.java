package com.zorroa.archivist.processors.export;

import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.processor.export.Port;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AssetInput exposes the asset and its current path as Output ports.
 */
public class AssetInput extends ExportProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AssetInput.class);

    public Port<Asset> inputAsset;
    public Port<String> outputPath;

    public AssetInput() {
        inputAsset = new Port<>("inputAsset", Port.Type.Input, this);
        outputPath = new Port<>("outputPath", Port.Type.Output, this);
    }

    @Override
    public void process() throws Exception {
        logger.info("{}", asset.getDocument());
        inputAsset.setValue(asset);
        outputPath.setValue(asset.getValue("source.path"));
    }
}
