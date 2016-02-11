package com.zorroa.archivist.aggregators;

import com.zorroa.archivist.sdk.domain.Ingest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by chambers on 2/11/16.
 */
public abstract class Aggregator {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public void init(Ingest ingest) { }
    public void teardown() { }

    public abstract void aggregate();
}
