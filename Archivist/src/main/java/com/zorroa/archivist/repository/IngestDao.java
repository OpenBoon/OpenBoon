package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.CreateIngestRequest;

public interface IngestDao {

    Ingest create(CreateIngestRequest req);

    Ingest get(String id);

}
