package com.zorroa.archivist.web;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.sdk.domain.User;
import com.zorroa.archivist.sdk.service.UserService;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
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

    @Autowired
    UserService userService;

    @Autowired
    SessionRegistry sessionRegistry;

    protected MockMvc mvc;

    @Before
    public void setup() {
        super.setup();
        this.mvc = MockMvcBuilders
                .webAppContextSetup(this.wac)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    private MockHttpSession buildSession(Authentication authentication) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, new MockSecurityContext(authentication));
        sessionRegistry.registerNewSession(session.getId(), authentication.getPrincipal());
        return session;
    }

    /**
     * @return a session for an employee with the id 2.
     */
    protected MockHttpSession user() {
        return user("user");
    }

    protected MockHttpSession user(String name) {
        User user = userService.get(name);
        return buildSession(new UnitTestAuthentication(user, user.getUsername(), userService.getPermissions(user)));
    }

    protected MockHttpSession user(Integer id) {
        User user = userService.get(id);
        return buildSession(new UnitTestAuthentication(user, user.getUsername(), userService.getPermissions(user)));
    }

    /**
     * @return a session for an admin with the id 1.
     */
    protected MockHttpSession admin() {
        return admin(1);
    }

    protected MockHttpSession admin(Integer id) {
        User user = userService.get(id);
        return buildSession(new UnitTestAuthentication(user, user.getUsername(), userService.getPermissions(user)));
    }
}
