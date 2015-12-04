package com.zorroa.archivist.sdk.exception;

import com.zorroa.archivist.sdk.domain.AssetBuilder;

/**
 * Created by chambers on 12/4/15.
 */
public class IngestProcessorException extends IngestException {

    private final AssetBuilder asset;
    private final Class<?> processor;

    public IngestProcessorException(String message, AssetBuilder asset, Class<?> processor) {
        super(message);
        this.asset = asset;
        this.processor = processor;
    }

    public AssetBuilder getAsset() {
        return asset;
    }

    public Class<?> getProcessor() {
        return processor;
    }
}
