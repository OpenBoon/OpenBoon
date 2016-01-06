package com.zorroa.archivist.ingestors;

import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.service.FolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ExportDateAggregator extends ExportProcessor {

    @Autowired
    FolderService folderService;

    @Override
    public void init(Export export, ExportOutput exportOutput) throws Exception {
        Folder exportsFolder;
        try {
            exportsFolder = folderService.get(0, "Exports");
        } catch (EmptyResultDataAccessException e) {
            FolderBuilder ingestBuilder = new FolderBuilder().setName("Exports").
                    setSearch(new AssetSearch());
            exportsFolder = folderService.create(ingestBuilder);
        }

        Date date = new Date(export.getTimeCreated());
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int year = cal.get(Calendar.YEAR);
        String yearName = Integer.toString(year);
        Folder yearFolder;
        try {
            yearFolder = folderService.get(exportsFolder.getId(), yearName);
        } catch (EmptyResultDataAccessException e) {
            yearFolder = folderService.create(new FolderBuilder().
                    setParentId(exportsFolder.getId()).setName(yearName));
        }

        int month = cal.get(Calendar.MONTH);
        String monthName = new DateFormatSymbols().getMonths()[month];
        Folder monthFolder;
        try {
            monthFolder = folderService.get(yearFolder.getId(), monthName);
        } catch (EmptyResultDataAccessException e) {
            monthFolder = folderService.create(new FolderBuilder().setName(monthName).setParentId(yearFolder.getId()));
        }

        String dayName = new SimpleDateFormat("EEEE, MMM dd").format(date);
        Folder dayFolder;
        try {
            dayFolder = folderService.get(monthFolder.getId(), dayName);
        } catch (EmptyResultDataAccessException e) {
            dayFolder = folderService.create(new FolderBuilder().setName(dayName)
                    .setParentId(monthFolder.getId()));
        }

        String ingestName = new SimpleDateFormat("hh:mm:ss a").format(date);
        try {
            folderService.get(dayFolder.getId(), ingestName);
        } catch (EmptyResultDataAccessException e) {
            AssetFilter ingestFilter = new AssetFilter().setExportId(export.getId());
            FolderBuilder ingestBuilder = new FolderBuilder().setName(ingestName).setSearch(new AssetSearch().setFilter(ingestFilter)).setParentId(dayFolder.getId());
            folderService.create(ingestBuilder);
        }
    }

    @Override
    public void process(ExportedAsset exportedAsset) throws Exception {

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
