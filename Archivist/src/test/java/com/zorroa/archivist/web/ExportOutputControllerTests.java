package com.zorroa.archivist.web;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.service.ExportService;
import com.zorroa.archivist.sdk.service.IngestService;
import com.zorroa.archivist.sdk.util.Json;
import com.zorroa.archivist.service.ExportExecutorService;
import com.zorroa.archivist.service.IngestExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 11/18/15.
 */
public class ExportOutputControllerTests extends MockMvcTest {


    @Autowired
    ExportService exportService;

    @Autowired
    IngestService ingestService;

    @Autowired
    IngestExecutorService ingestExecutorService;

    @Autowired
    ExportExecutorService exportExecutorService;

    Export export;

    ExportBuilder builder;

    @Before
    public void init() {

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex();

        ExportOptions options = new ExportOptions();
        options.getImages().setFormat("jpg");
        options.getImages().setScale(.5);

        AssetSearch search = new AssetSearch().setQuery("beer");

        ProcessorFactory<ExportProcessor> outputFactory = new ProcessorFactory<>();
        outputFactory.setKlass("com.zorroa.archivist.sdk.processor.export.ZipFileExport");

        builder = new ExportBuilder();
        builder.setNote("An export for Bob");
        builder.setOptions(options);
        builder.setSearch(search);
        builder.setOutputs(Lists.newArrayList(outputFactory));
        export = exportService.create(builder);

        exportExecutorService.execute(export);
    }

    @Test
    public void testGet() throws Exception {

        MockHttpSession session = admin();
        ExportOutput output = exportService.getAllOutputs(export).get(0);

        MvcResult result = mvc.perform(get("/api/v1/outputs/" + output.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isOk())
                .andReturn();

        ExportOutput output2 = Json.Mapper.readValue(result.getResponse().getContentAsString(), ExportOutput.class);
        assertEquals(output2, output);
    }

    @Test
    public void testDownload() throws Exception {

        MockHttpSession session = admin();
        ExportOutput output = exportService.getAllOutputs(export).get(0);

        MvcResult result = mvc.perform(get("/api/v1/outputs/" + output.getId() + "/_download")
                .session(session)
                .contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isOk())
                .andReturn();

        /*
         * Basically just asset the file we downloaded is the same as the file on disk.
         */
        int fileSize = result.getResponse().getContentLength();
        assertEquals(fileSize, Files.size(new File(output.getPath()).toPath()));
    }
}
