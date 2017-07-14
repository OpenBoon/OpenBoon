package com.zorroa.archivist.security;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Folder;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * These tests need an LDAP server setup.
 * Easiest way is ApacheDirectoryService
 * http://directory.apache.org/apacheds/basic-ug/1.4.3-adding-partition.html
 *
 * ### Determines if the Archivist LDAP plugin is enabled.
 * archivist.security.ldap.enabled=true
 *
 * ### The URL of the LDAP server.
 * archivist.security.ldap.url=ldap://localhost:10389
 *
 * ### The base of the LDAP query.
 * archivist.security.ldap.base=ou=people,o=sevenSeas
 *
 * ### A pattern for matching the username.
 * archivist.security.ldap.filter=(cn={0})
 *
 */
public class LdapAuthenticationTests extends AbstractTest {

    @Autowired
    AuthenticationManager authenticationManager;

    @Test
    @Ignore
    public void testLdapAuthentication() {

        Authentication a = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                "James Hook", "peterPan"));
        assertTrue(a.isAuthenticated());

        Folder userFolder = folderService.get("/Users/James Hook");
        assertEquals("admin", userFolder.getUser().getUsername());
    }

}
