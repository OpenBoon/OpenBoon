package com.zorroa.archivist.repository;

import java.util.List;

import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.domain.IngestPipeline;
import com.zorroa.archivist.domain.IngestState;
import com.zorroa.archivist.domain.ProxyConfig;

public interface IngestDao {

    Ingest get(long id);

    Ingest create(IngestPipeline pipeline, ProxyConfig config, IngestBuilder builder);

    List<Ingest> getAll();

    List<Ingest> getAll(IngestState state, int limit);

    boolean setState(Ingest ingest, IngestState newState, IngestState oldState);

}
