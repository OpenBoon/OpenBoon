package com.zorroa.archivist.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains the initialization methods so they don't get exposed on the
 * ObjectFileSystem interface.
 */
public abstract class AbstractFileSystem implements ObjectFileSystem {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public abstract void init();

}
