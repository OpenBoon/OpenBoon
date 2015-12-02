package com.zorroa.archivist.service;

import com.zorroa.archivist.repository.ExportDao;
import com.zorroa.archivist.repository.ExportOutputDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sun.plugin.dom.exception.InvalidStateException;

import java.util.List;

/**
 * Created by chambers on 11/1/15.
 */
@Service
@Transactional
public class ExportServiceImpl implements ExportService {

    @Autowired
    SearchService searchService;

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

    @Override
    public void restart(Export export) {

        /*
         * Try to reset the state first.  The current state must be finished.
         */
        if (!exportDao.setState(export, ExportState.Queued, ExportState.Finished)) {
            throw new InvalidStateException("Exports must be finished in order to be restarted.");
        }

        /*
         * The exports all get new output names so we don't generate new data on top of old
         * data that might be being accessed.
         */
        List<ExportOutput> outputs = exportOutputDao.getAll(export);
        for (ExportOutput output: outputs) {
            exportOutputDao.updateOutputPath(export, output);
        }
    }

    @Override
    public Export get(int id) {
        return exportDao.get(id);
    }

    @Override
    public List<ExportOutput> getAllOutputs(Export export) {
        return exportOutputDao.getAll(export);
    }

    @Override
    public ExportOutput getOutput(int i) {
        return exportOutputDao.get(i);
    }

    @Override
    public List<Export> getAll(ExportFilter filter) {
        return exportDao.getAll(filter);
    }

}

