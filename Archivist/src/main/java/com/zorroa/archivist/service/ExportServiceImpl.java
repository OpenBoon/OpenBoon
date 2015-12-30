package com.zorroa.archivist.service;

import com.google.common.collect.Lists;
import com.zorroa.archivist.ArchivistException;
import com.zorroa.archivist.repository.ExportDao;
import com.zorroa.archivist.repository.ExportOutputDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.service.EventLogService;
import com.zorroa.archivist.sdk.service.ExportService;
import com.zorroa.archivist.sdk.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by chambers on 11/1/15.
 */
@Service
@Transactional
public class ExportServiceImpl implements ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportServiceImpl.class);

    @Autowired
    EventLogService eventLogService;

    @Autowired
    SearchService searchService;

    @Autowired
    ExportDao exportDao;

    @Autowired
    ExportOutputDao exportOutputDao;

    @Value("${archivist.export.maxAssetCount}")
    int maxAssetCount;

    @Value("${archivist.export.maxTotalFileSize}")
    String maxTotalFileSize;

    @Override
    public Export create(ExportBuilder builder) {

        /*
         * Do the checks for maximum assets and file size.
         */
        long count = searchService.count(builder.getSearch()).getCount();
        if (count == 0) {
            throw new ArchivistException("The search did not match any assets.");
        }
        else if (count > maxAssetCount) {
            throw new ArchivistException(String.format("Cannot export more than '%d' assets at a time.", maxAssetCount));
        }

        long totalSize = searchService.getTotalFileSize(builder.getSearch());
        if (totalSize >  FileUtils.readbleSizeToBytes(maxTotalFileSize)) {
            throw new ArchivistException(String.format("Cannot export more than '%s' assets at a time.", maxTotalFileSize));
        }

        Export export = exportDao.create(builder, totalSize, count);
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
        if (!exportDao.setQueued(export)) {
            throw new ArchivistException("Exports must be finished in order to be restarted.");
        }

        /*
         * Change the search for the export to find all assets from the last export.  This is to ensure
         * our record of what was exported stays intact.
         */
        exportDao.setSearch(export, new AssetSearch().setFilter(
                new AssetFilter().setExportIds(Lists.newArrayList(export.getId()))));

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
    public Export duplicate(Export export) {
        /*
         * Build a new export from the given current export.
         */
        ExportBuilder builder = new ExportBuilder();
        builder.setSearch(export.getSearch());
        builder.setOutputs(exportOutputDao.getAll(export).stream().map(
                e->e.getFactory()).collect(Collectors.toList()));
        builder.setNote(export.getNote());
        builder.setOptions(export.getOptions());

        Export newExport = create(builder);
        return newExport;
    }

    @Override
    public boolean cancel(Export export) {
        return exportDao.setCancelled(export);
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

    @Override
    public boolean offline(ExportOutput output) {
        try {
            if (Files.deleteIfExists(new File(output.getPath()).toPath())) {
                exportOutputDao.setOffline(output);
                eventLogService.log(output, "Output was deleted: ", output);
                return true;
            }
            else {
                eventLogService.log("Could not delete export output, file did not exist. {}", output);
            }

        } catch (IOException e) {
            eventLogService.log("Failed to offline output: {}", e, output);
        }
        return false;
    }

    @Override
    public int offline(Export export) {
        int result = 0;
        for (ExportOutput output: getAllOutputs(export)) {
            if (offline(output)) {
                result++;
            }
        }
        return result;
    }
}

