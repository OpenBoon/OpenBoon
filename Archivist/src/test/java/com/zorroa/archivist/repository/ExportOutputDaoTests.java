package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 11/12/15.
 */
public class ExportOutputDaoTests extends ArchivistApplicationTests {

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

        AssetSearchBuilder search = new AssetSearchBuilder();
        search.setQuery("foo");

        ProcessorFactory<ExportProcessor> outputFactory = new ProcessorFactory<>();
        outputFactory.setKlass("com.zorroa.archivist.sdk.processor.export.ZipFileExport");

        ExportBuilder builder = new ExportBuilder();
        builder.setNote("An export for Bob");
        builder.setOptions(options);
        builder.setSearch(search);
        builder.setOutputs(Lists.newArrayList(outputFactory));

        export = exportDao.create(builder);
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

}
