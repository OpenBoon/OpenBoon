package com.zorroa.archivist.sdk.processor.ingest;

import com.google.common.collect.Sets;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.filesystem.ObjectFileSystem;
import com.zorroa.archivist.sdk.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public abstract class IngestProcessor extends Processor {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected Set<String> supportedFormats = Sets.newHashSet();

    protected ObjectFileSystem objectFileSystem;

    public IngestProcessor() { };

    public void init() {
        setArguments();
    }

    public abstract void process(AssetBuilder asset);

    /**
     * Return a set of all supported formats.  The point is this is to avoid opening or
     * attempting to open data that is not supported by the ingest pipeline.
     *
     * @return
     */
    public Set<String> supportedFormats() {
        return supportedFormats;
    }

    /**
     * Return true if the given string is a supported format.  If the supported
     * format set is empty, then this function also returns true.
     *
     * @param format
     * @return
     */
    public boolean isSupportedFormat(String format) {
        if (supportedFormats.isEmpty()) {
            return true;
        }
        return supportedFormats.contains(format.toLowerCase());
    }


    public ObjectFileSystem getObjectFileSystem() {
        return objectFileSystem;
    }

    public Processor setObjectFileSystem(ObjectFileSystem objectFileSystem) {
        this.objectFileSystem = objectFileSystem;
        return this;
    }
}
