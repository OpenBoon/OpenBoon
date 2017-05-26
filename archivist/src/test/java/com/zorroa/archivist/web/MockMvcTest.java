package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.security.UnitTestAuthentication;
import com.zorroa.sdk.util.Json;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;

public abstract class MockMvcTest extends AbstractTest {

    @Autowired
    protected WebApplicationContext wac;

    @Autowired
    protected FilterChainProxy springSecurityFilterChain;

    protected MockMvc mvc;

    @Before
    public void setup() throws IOException {
        super.setup();
        this.mvc = MockMvcBuilders
                .webAppContextSetup(this.wac)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    private MockHttpSession buildSession(Authentication authentication) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, new MockSecurityContext(authentication));

        return session;
    }

    protected <T> T deserialize(MvcResult result, Class<T> type) {
        return Json.deserialize(result.getResponse().getContentAsByteArray(), type);
    }

    protected <T> T deserialize(MvcResult result, TypeReference<T> type) {
        return Json.deserialize(result.getResponse().getContentAsByteArray(), type);
    }


    /**
     * @return a session for an employee with the id 2.
     */
    protected MockHttpSession user() {
        return user("user");
    }

    protected MockHttpSession user(String name) {
        User user = userService.get(name);
        return buildSession(new UnitTestAuthentication(user, userService.getPermissions(user)));
    }

    protected MockHttpSession user(Integer id) {
        User user = userService.get(id);
        return buildSession(new UnitTestAuthentication(user, userService.getPermissions(user)));
    }

    /**
     * @return a session for an admin with the id 1.
     */
    protected MockHttpSession admin() {
        return admin(1);
    }

    protected MockHttpSession admin(Integer id) {
        User user = userService.get(id);
        return buildSession(new UnitTestAuthentication(user, userService.getPermissions()));
    }

    public static class StatusResult<T> {
        public T object;
        public String op;
        public String id;
        public boolean success;
    }
}
