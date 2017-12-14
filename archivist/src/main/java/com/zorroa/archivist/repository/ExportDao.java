package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.ExportFile;
import com.zorroa.archivist.domain.ExportFileSpec;
import com.zorroa.archivist.domain.Job;

import java.util.List;

public interface ExportDao {
    ExportFile createExportFile(Job job, ExportFileSpec spec);

    ExportFile getExportFile(long id);

    List<ExportFile> getAllExportFiles(Job job);
}
