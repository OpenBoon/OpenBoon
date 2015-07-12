package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.*;

import java.util.List;

public interface IngestDao {

    Ingest get(long id);

    Ingest create(IngestPipeline pipeline, ProxyConfig config, IngestBuilder builder);

    List<Ingest> getAll();

    List<Ingest> getAll(IngestState state, int limit);

    List<Ingest> getAll(IngestFilter filter);

    boolean update(Ingest ingest, IngestUpdateBuilder builder);

    boolean setState(Ingest ingest, IngestState newState, IngestState oldState);

    boolean setState(Ingest ingest, IngestState newState);
}
