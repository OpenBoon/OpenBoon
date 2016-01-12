package com.zorroa.archivist.ingestors;

import com.zorroa.archivist.sdk.domain.Export;
import com.zorroa.archivist.sdk.domain.ExportOutput;
import com.zorroa.archivist.sdk.domain.ExportedAsset;
import com.zorroa.archivist.sdk.domain.Folder;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;

public class ExportDateAggregator extends ExportProcessor {

    private Folder exportFolder;

    @Autowired
    ExportService exportService;

    @Override
    public void init(Export export, ExportOutput exportOutput) throws Exception {
        exportFolder = exportService.getFolder(export);
    }

    @Override
    public void process(ExportedAsset exportedAsset) throws Exception {
        // Process paths underneath the export, like for ingests?
    }

    @Override
    public String getMimeType() {
        return null;
    }

    @Override
    public String getFileExtension() {
        return null;
    }

}
