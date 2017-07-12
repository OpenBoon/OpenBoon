package com.zorroa.archivist.web;

import com.zorroa.archivist.domain.Acl;
import com.zorroa.archivist.domain.Command;
import com.zorroa.archivist.domain.CommandSpec;
import com.zorroa.archivist.domain.CommandType;
import com.zorroa.archivist.service.CommandService;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.util.Json;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 4/24/17.
 */
public class CommandControllerTests extends MockMvcTest {

    @Autowired
    CommandService commandService;


    @Test
    public void testGet() throws Exception {
        MockHttpSession session = admin();
        CommandSpec spec = new CommandSpec();
        spec.setType(CommandType.UpdateAssetPermissions);
        spec.setArgs(new Object[] { new AssetSearch(), new Acl()});
        Command cmd = commandService.submit(spec);

        MvcResult result = mvc.perform(get("/api/v1/commands/" + cmd.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        Command cmd2 = Json.Mapper.readValue(result.getResponse().getContentAsString(), Command.class);
        assertEquals(cmd, cmd2);
    }

    @Test
    public void testCancel() throws Exception {
        MockHttpSession session = admin();
        CommandSpec spec = new CommandSpec();
        spec.setType(CommandType.Sleep);
        spec.setArgs(new Object[] { 5000L });
        Command cmd = commandService.submit(spec);
        commandService.setRunningCommand(cmd);

        MvcResult result = mvc.perform(put("/api/v1/commands/" + cmd.getId() + "/_cancel")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map<String,Object> rez = Json.Mapper.readValue(
                result.getResponse().getContentAsString(), Json.GENERIC_MAP);
        assertTrue((boolean) rez.get("success"));
    }
}
