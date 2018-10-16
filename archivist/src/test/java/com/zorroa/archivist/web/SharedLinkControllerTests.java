package com.zorroa.archivist.web;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.domain.SharedLink;
import com.zorroa.archivist.domain.SharedLinkSpec;
import com.zorroa.archivist.service.SharedLinkService;
import com.zorroa.common.util.Json;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MvcResult;

import static com.zorroa.archivist.security.UtilsKt.getUserId;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 7/11/17.
 */
public class SharedLinkControllerTests extends MockMvcTest {

    @Autowired
    SharedLinkService sharedLinkService;

    @Test
    public void testCreate() throws Exception {
        MockHttpSession session = admin();

        SharedLinkSpec spec = new SharedLinkSpec();
        spec.setSendEmail(false);
        spec.setState(ImmutableMap.of("foo", "bar"));
        spec.setUserIds(ImmutableSet.of(getUserId()));
        spec.setExpireTimeMs(1L);

        MvcResult result = mvc.perform(post("/api/v1/shared_link")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.INSTANCE.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        SharedLink t = deserialize(result, SharedLink.class);
        assertEquals(spec.getState(), t.getState());
    }

    @Test
    public void testGet() throws Exception {
        MockHttpSession session = admin();

        SharedLinkSpec spec = new SharedLinkSpec();
        spec.setSendEmail(false);
        spec.setState(ImmutableMap.of("foo", "bar"));
        spec.setUserIds(ImmutableSet.of(getUserId()));
        spec.setExpireTimeMs(1L);
        SharedLink link = sharedLinkService.create(spec);

        MvcResult result = mvc.perform(get("/api/v1/shared_link/" + link.getId())
                .session(session)
                .content(Json.INSTANCE.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        SharedLink t = deserialize(result, SharedLink.class);
        assertEquals(spec.getState(), t.getState());
    }
}
