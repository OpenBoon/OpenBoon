package com.zorroa.archivist.processors;

import com.google.common.collect.Maps;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.AssetType;
import com.zorroa.archivist.sdk.domain.Ingest;
import com.zorroa.archivist.sdk.exception.UnrecoverableIngestProcessorException;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumMap;

/**
 * SchemaAssetMetadataProcessor delegates the processing of specific asset
 * types to other processors designed to handle that type.  This is one way
 * to avoid one giant processor for all types.
 *
 * @author chambers
 *
 */
public class AssetMetadataProcessor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AssetMetadataProcessor.class);

    private final EnumMap<AssetType, IngestProcessor> assetTypeProcessors = Maps.newEnumMap(AssetType.class);

    @Autowired
    private ImageService imageService;

    @Override
    public void init(Ingest ingest) {
        /*
         * Set up the delegate processors.
         */
        assetTypeProcessors.put(AssetType.Image, new ImageProcessor(imageService));
    }

    @Override
    public void process(AssetBuilder asset) {
        try {
            asset.getSource().setFileSize(Files.size(asset.getFile().toPath()));
        } catch (IOException e) {
            throw new UnrecoverableIngestProcessorException("Unable to determine file size", getClass());
        }

        IngestProcessor delegate = assetTypeProcessors.get(asset.getSource().getType());
        if (delegate == null) {
            throw new UnrecoverableIngestProcessorException("Unsupported file type: "
                    + asset.getExtension(), getClass());
        }

        delegate.process(asset);
    }
}
