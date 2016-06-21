package com.zorroa.archivist.service;

import com.zorroa.sdk.domain.Ingest;

/**
 * Created by chambers on 6/21/16.
 */
public interface AggregationService {
    void invalidate(Ingest ingest);

    void aggregate(Ingest ingest);
}
