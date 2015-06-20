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

    Ingest getNextWaitingIngest();

    void incrementCreatedCount(Ingest ingest, int increment);

    void incrementErrorCount(Ingest ingest, int increment);

    boolean setRunning(Ingest ingest);

    boolean setFinished(Ingest ingest);

    List<Ingest> getPending();

    void setState(Ingest ingest, IngestState state);

}
