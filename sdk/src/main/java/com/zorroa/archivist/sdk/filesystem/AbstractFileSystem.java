package com.zorroa.archivist.sdk.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Contains the initialization methods so they don't get exposed on the
 * ObjectFileSystem interface.
 */
public abstract class AbstractFileSystem implements ObjectFileSystem {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Properties properties;

    public AbstractFileSystem(Properties properties) {
        this.properties = properties;
    }

    /**
     * Do any kind of root directory creation or authentication here.
     */
    public abstract void init();

}
