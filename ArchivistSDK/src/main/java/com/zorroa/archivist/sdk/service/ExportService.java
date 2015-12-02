package com.zorroa.archivist.sdk.service;

import com.zorroa.archivist.sdk.domain.Export;
import com.zorroa.archivist.sdk.domain.ExportBuilder;
import com.zorroa.archivist.sdk.domain.ExportFilter;
import com.zorroa.archivist.sdk.domain.ExportOutput;

import java.util.List;

/**
 * Created by chambers on 11/1/15.
 */
public interface ExportService {

    Export create(ExportBuilder builder);

    Export get(int id);

    List<ExportOutput> getAllOutputs(Export export);

    ExportOutput getOutput(int id);

    List<Export> getAll(ExportFilter filter);

    void restart(Export export);

}
