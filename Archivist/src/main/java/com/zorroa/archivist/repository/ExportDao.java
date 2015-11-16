package com.zorroa.archivist.repository;

import com.zorroa.archivist.sdk.domain.Export;
import com.zorroa.archivist.sdk.domain.ExportBuilder;
import com.zorroa.archivist.sdk.domain.ExportState;

/**
 * Created by chambers on 11/12/15.
 */
public interface ExportDao {

    Export get(int id);

    Export create(ExportBuilder builder);

    boolean setState(Export export, ExportState newState, ExportState oldState);
}
