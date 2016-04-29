package com.zorroa.archivist.aggregators;

import com.zorroa.sdk.domain.Export;
import com.zorroa.sdk.domain.ExportOutput;
import com.zorroa.sdk.domain.ExportedAsset;
import com.zorroa.sdk.domain.Folder;
import com.zorroa.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.service.ExportService;
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
