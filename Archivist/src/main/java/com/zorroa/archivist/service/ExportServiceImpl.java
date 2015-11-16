package com.zorroa.archivist.service;

import com.zorroa.archivist.repository.ExportDao;
import com.zorroa.archivist.repository.ExportOutputDao;
import com.zorroa.archivist.sdk.domain.Export;
import com.zorroa.archivist.sdk.domain.ExportBuilder;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by chambers on 11/1/15.
 */
@Service
@Transactional
public class ExportServiceImpl implements ExportService {

    @Autowired
    ExportDao exportDao;

    @Autowired
    ExportOutputDao exportOutputDao;

    @Override
    public Export create(ExportBuilder builder) {
        Export export = exportDao.create(builder);
        for (ProcessorFactory<ExportProcessor> factory: builder.getOutputs()) {
            exportOutputDao.create(export, factory);
        }
        return export;
    }
}

