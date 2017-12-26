package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.ExportFile;
import com.zorroa.archivist.domain.ExportFileSpec;
import com.zorroa.archivist.domain.ExportSpec;
import com.zorroa.archivist.domain.Job;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;

import java.util.List;

/**
 * Created by chambers on 11/1/15.
 */
public interface ExportService {

    ExportFile createExportFile(Job job, ExportFileSpec spec);

    ExportFile getExportFile(long fileId);

    List<ExportFile> getAllExportFiles(Job job);

    Job create(ExportSpec spec);

    PagedList<Job> getAll(Pager page);
}
