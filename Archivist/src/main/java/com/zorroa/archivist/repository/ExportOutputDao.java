package com.zorroa.archivist.repository;

import com.zorroa.archivist.sdk.domain.Export;
import com.zorroa.archivist.sdk.domain.ExportOutput;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;

import java.util.List;

/**
 * Created by chambers on 11/12/15.
 */
public interface ExportOutputDao {
    ExportOutput get(int id);

    List<ExportOutput> getAll(Export export);

    ExportOutput create(Export export,  ProcessorFactory<ExportProcessor> output);

    void updateOutputPath(Export export, ExportOutput output);

    /**
     * Set the file size on an export output.  This is only known after the files are processed.
     *
     * @param output
     * @param size
     */
    void setFileSize(ExportOutput output, long size);
}
