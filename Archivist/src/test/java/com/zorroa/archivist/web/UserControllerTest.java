package com.zorroa.archivist.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.web.WebAppConfiguration;

@WebAppConfiguration
public class UserControllerTest extends MockMvcTest {

    @Autowired
    UserController userController;

    @Test
    public void testLogin() throws Exception {
        mvc.perform(post("/api/v1/login").session(admin())).andExpect(status().isOk());
    }
}
