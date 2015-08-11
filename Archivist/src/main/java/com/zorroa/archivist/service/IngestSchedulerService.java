package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Ingest;

public interface IngestSchedulerService {

    boolean executeIngest(Ingest ingest);

    boolean resume(Ingest ingest);

    boolean pause(Ingest ingest);

    boolean stop(Ingest ingest);
}
