package com.zorroa.archivist.service;

import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.Export;
import com.zorroa.archivist.sdk.domain.ExportOutput;
import com.zorroa.archivist.sdk.domain.ExportedAsset;

/**
 * Created by chambers on 11/23/15.
 */
public interface ExportOptionsService {

    /**
     * Apply all the export options for a given asset and return an ExportedAsset instance.
     *
     * @param export
     * @param output
     * @param asset
     * @return
     * @throws Exception
     */
    ExportedAsset applyOptions(Export export, ExportOutput output, Asset asset) throws Exception;
}
