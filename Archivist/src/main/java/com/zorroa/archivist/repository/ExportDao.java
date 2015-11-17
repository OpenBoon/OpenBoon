package com.zorroa.archivist.repository;

import com.zorroa.archivist.sdk.domain.Export;
import com.zorroa.archivist.sdk.domain.ExportBuilder;
import com.zorroa.archivist.sdk.domain.ExportFilter;
import com.zorroa.archivist.sdk.domain.ExportState;

import java.util.List;

/**
 * Created by chambers on 11/12/15.
 */
public interface ExportDao {

    Export get(int id);

    List<Export> getAll(ExportFilter filter);

    Export create(ExportBuilder builder);

    List<Export> getAll(ExportState state, int limit);

    boolean setState(Export export, ExportState newState, ExportState oldState);
}
