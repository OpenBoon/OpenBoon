package com.zorroa.archivist.sdk.exception;

/**
 * An exception for skipping a particular asset.
 */
public class SkipIngestException extends IngestException {

    public SkipIngestException(String message) {
        super(message);
    }

    public SkipIngestException(String message, Throwable cause) {
        super(message, cause);
    }
}
