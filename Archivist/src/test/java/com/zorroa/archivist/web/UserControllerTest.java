package com.zorroa.archivist.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.zorroa.archivist.ArchivistApplicationTests;

@WebAppConfiguration
public class UserControllerTest extends ArchivistApplicationTests {

    @Autowired
    WebApplicationContext wac;

    MockMvc mvc;

    @Autowired
    Filter springSecurityFilterChain;

    @Before
    public void setup() {
        this.mvc = MockMvcBuilders
                .webAppContextSetup(this.wac)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    public void testLogin() throws Exception {
    	
    	//List<GrantedAuthority> list = new ArrayList<GrantedAuthority>();
    	//list.add(new GrantedAuthorityImpl("ROLE_ADMIN"));        
    	//UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("admin", "admin", list);
    	//SecurityContextHolder.getContext().setAuthentication(auth);
    	
        mvc.perform(post("/login").param("username", "admin").param("password", "admin")).andExpect(status().isOk());
    }

}
