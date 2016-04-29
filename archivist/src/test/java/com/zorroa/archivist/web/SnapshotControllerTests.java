package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.domain.Snapshot;
import com.zorroa.archivist.domain.SnapshotBuilder;
import com.zorroa.archivist.domain.SnapshotRestoreBuilder;
import com.zorroa.archivist.domain.SnapshotState;
import com.zorroa.sdk.domain.Ingest;
import com.zorroa.sdk.domain.IngestBuilder;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SnapshotControllerTests extends MockMvcTest {

    @Ignore
    @Test
    public void testSnapshotCreateRestoreGetDelete() throws Exception {
        /**
         * TODO: will fail until the snapshotting is updated for elastic 1.7
         */
        // Start by ingesting the standard test assets
        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex();

        MockHttpSession session = admin();

        // Snapshot the whole archivist index
        SnapshotBuilder builder = new SnapshotBuilder();
        String name = "test";
        builder.setName(name);
        MvcResult result = mvc.perform(post("/api/v1/snapshots")
                .session(session)
                .content(Json.serialize(builder))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        Snapshot snapshot = Json.Mapper.readValue(result.getResponse().getContentAsString(), Snapshot.class);
        assertEquals(snapshot.getName(), name);

        // Wait until the snapshot is finished
        while (snapshot.getState() == SnapshotState.InProgress) {
            result = mvc.perform(get("/api/v1/snapshots/" + name)
                    .session(session))
                    .andExpect(status().isOk())
                    .andReturn();
            snapshot = Json.Mapper.readValue(result.getResponse().getContentAsString(), Snapshot.class);
            refreshIndex();
        }

        // Restore the snapshot to a separate index
        SnapshotRestoreBuilder restore = new SnapshotRestoreBuilder();
        restore.setRenamePattern("archivist_(.+)");
        restore.setRenameReplacement("restored_archivist_$1");
        result = mvc.perform(put("/api/v1/snapshots/" + name + "/_restore")
                .session(session)
                .content(Json.serialize(restore))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        Boolean ok = Json.Mapper.readValue(result.getResponse().getContentAsString(), Boolean.class);
        assertTrue(ok);

        // Wait until the snapshot is finished
        do {
            result = mvc.perform(get("/api/v1/snapshots/" + name)
                    .session(session))
                    .andExpect(status().isOk())
                    .andReturn();
            snapshot = Json.Mapper.readValue(result.getResponse().getContentAsString(), Snapshot.class);
            refreshIndex();
        } while (snapshot.getState() == SnapshotState.InProgress);

        // Count the number of assets in the newly restored index
        CountRequestBuilder countBuilder = client.prepareCount("restored_archivist_01")
                .setTypes("asset");
        String query = "{ \"query\": { \"match_all\" : {} } }";
        countBuilder.setSource(query.getBytes());
        CountResponse response = countBuilder.get();

        assertEquals(2, (int) response.getCount());

        // Get all snapshots and make sure we have only one
        result = mvc.perform(get("/api/v1/snapshots")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        List<Snapshot> snapshots = Json.Mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<Snapshot>>() {});
        assertEquals(snapshots.size(), 1);

        // Delete the snapshot
        result = mvc.perform(delete("/api/v1/snapshots/" + name)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        ok = Json.Mapper.readValue(result.getResponse().getContentAsString(), Boolean.class);
        assertTrue(ok);
        refreshIndex();

        // Get all snapshots and verify that we deleted the one we made
        result = mvc.perform(get("/api/v1/snapshots")
                .session(session))
                .andExpect(status().isOk())
                .andReturn();
        snapshots = Json.Mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<Snapshot>>() {});
        assertEquals(snapshots.size(), 0);

    }

    @Test
    public void testDbBackup() throws Exception {
        MockHttpSession session = admin();
        MvcResult result = mvc.perform(post("/api/v1/db/_backup")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> data = Json.deserialize(result.getResponse().getContentAsString(), Json.GENERIC_MAP);
        assertTrue(data.containsKey("path"));

        File file = new File((String) data.get("path"));
        assertTrue(file.exists());
        Files.deleteIfExists(file.toPath());
    }
}
