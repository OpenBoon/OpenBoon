package com.zorroa.archivist.processors;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.ingest.IngestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ChecksumProcessor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AssetMetadataProcessor.class);

    @Override
    public void process(AssetBuilder asset) {
        try {
            byte[] bytes = Files.toByteArray(asset.getFile());
            asset.put("source", "hash", Hashing.murmur3_128().hashBytes(bytes).toString());
        } catch (IOException e) {
            logger.warn("Failed to calculate CRC for file '{}'", asset.getAbsolutePath(), e);
        }
    }

}
