package com.zorroa.archivist.service;

import com.zorroa.archivist.sdk.domain.Export;

/**
 * Created by chambers on 11/2/15.
 */
public interface ExportExecutorService {

    void execute(Export export);
}
