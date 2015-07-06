package com.zorroa.archivist.repository;

import java.util.List;

import com.zorroa.archivist.domain.*;

public interface IngestDao {

    Ingest get(long id);

    Ingest create(IngestPipeline pipeline, ProxyConfig config, IngestBuilder builder);

    List<Ingest> getAll();

    List<Ingest> getAll(IngestState state, int limit);

    List<Ingest> getAll(IngestFilter filter);

    boolean setState(Ingest ingest, IngestState newState, IngestState oldState);

}
