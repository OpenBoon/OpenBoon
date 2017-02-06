package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.service.AnalystService;
import com.zorroa.common.domain.Analyst;
import com.zorroa.common.domain.AnalystSpec;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.util.Json;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 3/3/16.
 */
public class AnalystControllerTests extends MockMvcTest {

    @Autowired
    AnalystService analystService;

    AnalystSpec ping;

    @Before
    public void init() {
        ping = sendAnalystPing();
    }

    @Test
    public void testGetAnalysts() throws Exception {

        MockHttpSession session = admin();

        MvcResult result = mvc.perform(get("/api/v1/analysts")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        PagedList<Analyst> analysts = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<PagedList<Analyst>>() {});
        assertEquals(1, analysts.size());
    }
}
