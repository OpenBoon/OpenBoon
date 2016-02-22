package com.zorroa.archivist.service;

import com.zorroa.archivist.aggregators.Aggregator;
import com.zorroa.archivist.sdk.domain.Ingest;

import java.util.List;

public interface IngestExecutorService {

    boolean executeIngest(Ingest ingest);

    boolean resume(Ingest ingest);

    boolean pause(Ingest ingest);

    boolean stop(Ingest ingest);

    List<Aggregator> getAggregators(Ingest ingest);
}
