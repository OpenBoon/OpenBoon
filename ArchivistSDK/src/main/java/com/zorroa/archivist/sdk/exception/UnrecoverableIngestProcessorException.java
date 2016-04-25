package com.zorroa.archivist.sdk.exception;

/**
 * If thrown from an IngestProcessor, the asset being processed will be skipped.
 */
public class UnrecoverableIngestProcessorException extends IngestException {

    private final Class<?> processor;

    public UnrecoverableIngestProcessorException(Class<?> processor) {
        super();
        this.processor = processor;
    }

    public UnrecoverableIngestProcessorException(String message, Class<?> processor) {
        super(message);
        this.processor = processor;
    }

    public UnrecoverableIngestProcessorException(String message, Throwable cause, Class<?> processor) {
        super(message, cause);
        this.processor = processor;
    }

    public UnrecoverableIngestProcessorException(Throwable cause, Class<?> processor) {
        super(cause);
        this.processor = processor;
    }

    public Class<?> getProcessor() {
        return processor;
    }
}
