package com.zorroa.archivist.sdk.processor;

import com.zorroa.archivist.sdk.domain.Ingest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by chambers on 2/11/16.
 */
public abstract class Aggregator extends Processor {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public void init(Ingest ingest) {
        setArguments();
    }

    public void teardown() { }

    public abstract void aggregate();
}
