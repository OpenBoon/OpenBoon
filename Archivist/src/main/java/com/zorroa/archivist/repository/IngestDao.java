package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.domain.IngestPipeline;

public interface IngestDao {

    Ingest get(long id);

    Ingest create(IngestPipeline pipeline, IngestBuilder builder);

}
