package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.CreateIngestRequest;
import com.zorroa.archivist.domain.IngestState;

public interface IngestDao {

    Ingest create(CreateIngestRequest req);

    Ingest get(String id);

    Ingest getNext();

    void setState(Ingest ingest, IngestState state);

    void start(Ingest ingest);

}
