package com.zorroa.archivist.service;

import com.zorroa.sdk.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by chambers on 7/8/16.
 */
@Service
public class ExportServiceImpl implements ExportService {

    @Override
    public Export create(ExportBuilder builder) {
        return null;
    }

    @Override
    public Export get(int id) {
        return null;
    }

    @Override
    public List<ExportOutput> getAllOutputs(Export export) {
        return null;
    }

    @Override
    public ExportOutput getOutput(int id) {
        return null;
    }

    @Override
    public List<Export> getAll(ExportFilter filter) {
        return null;
    }

    @Override
    public void restart(Export export) {

    }

    @Override
    public Export duplicate(Export export) {
        return null;
    }

    @Override
    public boolean cancel(Export export) {
        return false;
    }

    @Override
    public boolean offline(ExportOutput output) {
        return false;
    }

    @Override
    public int offline(Export export) {
        return 0;
    }

    @Override
    public Folder getFolder(Export export) {
        return null;
    }
}
