package com.zorroa.archivist.web;

import com.zorroa.archivist.ArchivistApplicationTests;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

public abstract class MockMvcTest extends ArchivistApplicationTests {

    @Autowired
    protected WebApplicationContext wac;

    @Autowired
    protected FilterChainProxy springSecurityFilterChain;

    protected MockMvc mvc;

    @Before
    public void setup() {
        this.mvc = MockMvcBuilders
                .webAppContextSetup(this.wac)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    private MockHttpSession buildSession(Authentication authentication) {
        logger.info("Creating new session");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, new MockSecurityContext(authentication));
        return session;
    }

    /**
     * @return a session for an employee with the id 2.
     */
    protected MockHttpSession user() {
        return user(2);
    }

    protected MockHttpSession user(Integer id) {
        return buildSession(AuthenticationMocks.userAuthentication(id));
    }

    /**
     * @return a session for an admin with the id 1.
     */
    protected MockHttpSession admin() {
        return admin(1);
    }

    protected MockHttpSession admin(Integer id) {
        return buildSession(AuthenticationMocks.adminAuthentication(id));
    }
}
