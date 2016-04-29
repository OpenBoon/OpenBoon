package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 11/12/15.
 */
public class ExportOutputDaoTests extends AbstractTest {

    @Autowired
    ExportDao exportDao;

    @Autowired
    ExportOutputDao exportOutputDao;

    Export export;

    ExportOutput output;

    @Before
    public void init() {

        ExportOptions options = new ExportOptions();
        options.getImages().setFormat("jpg");
        options.getImages().setScale(.5);

        AssetSearch search = new AssetSearch();
        search.setQuery("foo");

        ProcessorFactory<ExportProcessor> outputFactory = new ProcessorFactory<>();
        outputFactory.setKlass("com.zorroa.archivist.sdk.processor.export.ZipFileExport");

        ExportBuilder builder = new ExportBuilder();
        builder.setNote("An export for Bob");
        builder.setOptions(options);
        builder.setSearch(search);
        builder.setOutputs(Lists.newArrayList(outputFactory));

        export = exportDao.create(builder, 0, 0);
        output = exportOutputDao.create(export, outputFactory);
    }

    @Test
    public void testGet() {
        ExportOutput output2 = exportOutputDao.get(output.getId());
        assertEquals(output2, output);
    }

    @Test
    public void testGetAll() {
        List<ExportOutput> outputs = exportOutputDao.getAll(export);
        assertEquals(1, outputs.size());
    }

    @Test
    public void testSetFileSize() {
        int size = 1000;
        exportOutputDao.setFileSize(output, size);
        ExportOutput output2 = exportOutputDao.get(output.getId());
        assertEquals(size, output2.getFileSize());
    }

    @Test
    public void testGetAllExpired() throws InterruptedException {
        assertTrue(exportDao.setRunning(export));
        assertTrue(exportDao.setFinished(export));
        exportOutputDao.setOnline(output);
        Thread.sleep(100);

        assertEquals(0, exportOutputDao.getAllExpired(1000).size());
        assertEquals(1, exportOutputDao.getAllExpired(50).size());
    }

    @Test
    public void testsStOnlineOfflineStatus() {
        assertTrue(exportOutputDao.setOnline(output));
        assertFalse(exportOutputDao.setOnline(output));
        assertTrue(exportOutputDao.setOffline(output));
        assertFalse(exportOutputDao.setOffline(output));
    }
}
