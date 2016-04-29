package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.processor.ProcessorFactory;
import com.zorroa.sdk.processor.export.ExportProcessor;
import org.elasticsearch.action.count.CountResponse;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.File;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by chambers on 11/16/15.
 */
public class ExportExecutorServiceTests extends AbstractTest {

    Export export;

    @Before
    public void init() {

        addTestAssets("standard");

        ExportOptions options = new ExportOptions();
        options.getImages().setFormat("jpg");
        options.getImages().setScale(.5);

        AssetSearch search = new AssetSearch();
        search.setQuery("beer");

        ProcessorFactory<ExportProcessor> outputFactory = new ProcessorFactory<>();
        outputFactory.setKlass("com.zorroa.sdk.processor.export.ZipFileExport");
        outputFactory.setArgs(ImmutableMap.of("zipEntryPath", ""));

        ExportBuilder builder = new ExportBuilder();
        builder.setNote("An export for Bob");
        builder.setOptions(options);
        builder.setSearch(search);
        builder.setOutputs(Lists.newArrayList(outputFactory));

        export = exportService.create(builder);
    }

    @Test
    public void testExecuteExport() {

        /**
         * Log out the current user to ensure the test authenticates.
         */
        SecurityContextHolder.getContext().setAuthentication(null);

        exportExecutorService.execute(export);
        refreshIndex(1000);

        /**
         * The export executor service logs the user out after
         * exporting, which will the rest of this test.  So
         * we'll just re-authenticate.
         */
        authenticate();

        List<Integer> exports = new ArrayList<>();
        exports.add(export.getId());

        AssetFilter filter = new AssetFilter().setExportIds(exports);
        AssetSearch search = new AssetSearch().setFilter(filter);

        // Assert the export id has been added to the asset.
        assertEquals(1, searchService.search(search).getHits().getHits().length);

        List<ExportOutput> outputs = exportService.getAllOutputs(export);
        assertTrue(new File(outputs.get(0).getPath()).exists());
    }

    @Test
    public void testCancelExport() {

        /**
         * Log out the current user to ensure the test authenticates.
         */
        SecurityContextHolder.getContext().setAuthentication(null);

        assertTrue(exportService.cancel(export));
        exportExecutorService.execute(export);

        Export export2 = exportService.get(export.getId());
        assertEquals(1, export2.getAssetCount());
        assertEquals(ExportState.Cancelled, export2.getState());
        assertTrue(export2.getTimeStopped() > -1);
    }

    @Test
    public void testExportAggregator() {
        /**
         * Log out the current user to ensure the test authenticates.
         */
        SecurityContextHolder.getContext().setAuthentication(null);

        exportExecutorService.execute(export);

        authenticate();

        Date date = new Date(export.getTimeCreated());
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        String yearName = Integer.toString(year);
        int month = cal.get(Calendar.MONTH);
        String monthName = new DateFormatSymbols().getMonths()[month];

        String path = "/Exports/" + yearName + "/" + monthName;
        Folder exportMonthFolder = folderService.get(path);
        assertNotEquals(null, exportMonthFolder);

        refreshIndex(1000);

        AssetSearch search = new AssetSearch().setFilter(new AssetFilter().setFolderId(exportMonthFolder.getId()));
        CountResponse response = searchService.count(search);
        assertEquals(1, response.getCount());
    }
}
