package com.zorroa.archivist.web;

import com.google.common.collect.ImmutableList;
import com.zorroa.archivist.domain.EventLogSearch;
import com.zorroa.archivist.domain.Task;
import com.zorroa.archivist.domain.UserLogSpec;
import com.zorroa.archivist.service.EventLogService;
import com.zorroa.common.cluster.thrift.StackElementT;
import com.zorroa.common.cluster.thrift.TaskErrorT;
import com.zorroa.sdk.util.Json;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 8/30/16.
 */
public class EventLogControllerTests extends MockMvcTest {

    @Autowired
    EventLogService logService;

    @Test
    public void testEmptyUserLogSearch() throws Exception {
        logService.log(new UserLogSpec().setAction("test").setMessage("A log test"));
        refreshIndex();

        EventLogSearch search = new EventLogSearch();
        MockHttpSession session = admin();
        MvcResult result = mvc.perform(post("/api/v1/eventlogs/user/_search")
                .session(session)
                .content(Json.serializeToString(search))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map sr = deserialize(result, Map.class);
        List<Map<String, Object>> list = ((List) sr.get("list"));
        assertTrue(list.size() > 0);

        boolean testActionFound = false;
        for (Map<String,Object> entry: list) {
            if (entry.get("action").toString().equals("test")) {
                testActionFound = true;
                break;
            }
        }
        assertTrue(testActionFound);
    }

    @Test
    public void testEmptyJobLogSearch() throws Exception {

        Task task = new Task();
        task.setJobId(5);
        task.setTaskId(200);

        TaskErrorT error = new TaskErrorT();
        error.setPath("/bar/bing");
        error.setTimestamp(System.currentTimeMillis());
        error.setOriginPath("/shoe");
        error.setOriginService("test");
        error.setPhase("process");
        error.setSkipped(true);
        error.setMessage("There was a failure");
        error.setStack(Lists.newArrayList(new StackElementT()
                .setClassName("com.zorroa.core.plugins.Processor")
                .setMethod("foo")
                .setLineNumber(10)
                .setFile("Processor.class")));

        logService.log(task, ImmutableList.of(error));
        refreshIndex();

        EventLogSearch search = new EventLogSearch();
        MockHttpSession session = admin();
        MvcResult result = mvc.perform(post("/api/v1/eventlogs/job/_search")
                .session(session)
                .content(Json.serializeToString(search))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map sr = deserialize(result, Map.class);
        List<Map<String, Object>> list = ((List) sr.get("list"));
        assertEquals(1, list.size());
    }
}
