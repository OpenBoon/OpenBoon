package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Ingest;

public interface IngestSchedulerService {

    Ingest executeNextIngest();

    void executeIngest(Ingest ingest);
}
