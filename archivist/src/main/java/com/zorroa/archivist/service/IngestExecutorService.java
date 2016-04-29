package com.zorroa.archivist.service;

import com.zorroa.sdk.domain.Ingest;

public interface IngestExecutorService {

    boolean executeIngest(Ingest ingest);

    boolean resume(Ingest ingest);

    boolean pause(Ingest ingest);

    boolean stop(Ingest ingest);
}
