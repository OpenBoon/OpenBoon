package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.zorroa.archivist.repository.ExportDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.service.ExportService;
import com.zorroa.archivist.sdk.service.IngestService;
import com.zorroa.archivist.sdk.util.Json;
import com.zorroa.archivist.service.IngestExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 11/17/15.
 */
public class ExportControllerTests extends MockMvcTest {

    @Autowired
    ExportService exportService;

    @Autowired
    IngestService ingestService;

    @Autowired
    ExportDao exportDao;

    @Autowired
    IngestExecutorService ingestExecutorService;

    Export export;

    ExportBuilder builder;

    MockHttpSession session;

    @Before
    public void init() {
        session = admin();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex(1000);

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
    }

    @Test
    public void testGetAllByFilter() throws Exception {
        ExportFilter filter = new ExportFilter();

        MvcResult result = mvc.perform(post("/api/v1/exports/_search")
                .session(session)
                .content(Json.serialize(filter))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<Export> exports = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<Export>>() {});
        assertEquals(1, exports.size());
    }

    @Test
    public void testCreate() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/exports")
                .session(session)
                .content(Json.serialize(builder))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Export export2 = Json.Mapper.readValue(result.getResponse().getContentAsString(), Export.class);
        assertNotEquals(export.getId(), export2.getId());
        assertEquals("beer", export2.getSearch().getQuery());
        assertEquals(export2.getNote(), export.getNote());
    }

    @Test
    public void testGet() throws Exception {

        MvcResult result = mvc.perform(get("/api/v1/exports/" + export.getId())
                .session(session)
                .content(Json.serialize(builder))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Export export2 = Json.Mapper.readValue(result.getResponse().getContentAsString(), Export.class);
        assertEquals(export.getId(), export2.getId());
        assertEquals("beer", export2.getSearch().getQuery());
        assertEquals(export2.getNote(), export.getNote());
    }

    @Test
    public void testRestart() throws Exception {
        /*
         * artificially set the export to finished.
         */
        exportDao.setState(export, ExportState.Finished, ExportState.Queued);

        MvcResult result = mvc.perform(put("/api/v1/exports/" + export.getId() + "/_restart")
                .session(session)
                .content(Json.serialize(builder))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Export export2 = Json.Mapper.readValue(result.getResponse().getContentAsString(), Export.class);
        assertEquals(ExportState.Queued, export2.getState());
    }

    @Test
    public void testGetAllOutputs() throws Exception {

        MvcResult result = mvc.perform(get("/api/v1/exports/" + export.getId() + "/outputs")
                .session(session)
                .content(Json.serialize(builder))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<ExportOutput> outputs = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<ExportOutput>>() { });
        assertEquals(1, outputs.size());
    }

    @Test
    public void testCancel() throws Exception {
        /*
         * artificially set the export to running.
         */
        exportDao.setState(export, ExportState.Running, ExportState.Queued);

        MvcResult result = mvc.perform(put("/api/v1/exports/" + export.getId() + "/_cancel")
                .session(session)
                .content(Json.serialize(builder))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Export export2 = Json.Mapper.readValue(result.getResponse().getContentAsString(), Export.class);
        assertEquals(ExportState.Cancelled, export2.getState());
    }
}
