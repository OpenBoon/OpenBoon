package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Ingest;

public interface IngestSchedulerService {

    Ingest executeNextIngest();

    void executeIngest(Ingest ingest);

    void restart(Ingest ingest, boolean paused);

    boolean pause(Ingest ingest);

    boolean resume(Ingest ingest);
}
