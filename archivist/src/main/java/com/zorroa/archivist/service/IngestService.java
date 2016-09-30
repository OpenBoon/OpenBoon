package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestSpec;
import com.zorroa.archivist.domain.Job;

import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;

import java.util.List;

/**
 * Created by chambers on 7/9/16.
 */
public interface IngestService {
    Ingest create(IngestSpec spec);

    Job spawnImportJob(Ingest ingest);

    boolean update(int id, Ingest spec);

    boolean delete(int id);

    List<Ingest> getAll();

    PagedList<Ingest> getAll(Pager page);

    Ingest get(int id);

    Ingest get(String name);

    long count();

    boolean exists(String name);
}
