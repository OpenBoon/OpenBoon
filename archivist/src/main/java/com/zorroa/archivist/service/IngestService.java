package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestSpec;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;

import java.util.List;

/**
 * Created by chambers on 7/9/16.
 */
public interface IngestService {
    Ingest create(IngestSpec spec);

    void spawnImportJob(Ingest ingest);

    boolean update(int id, Ingest spec);

    boolean delete(int id);

    List<Ingest> getAll();

    PagedList<Ingest> getAll(Paging page);

    Ingest get(int id);

    Ingest get(String name);

    long count();

    boolean exists(String name);
}
