/*
 * Copyright 2017 Vincenzo De Notaris
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zorroa.security.saml;

import com.zorroa.archivist.sdk.security.AuthSource;
import com.zorroa.archivist.sdk.security.UserRegistryService;
import org.opensaml.saml2.core.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.metadata.MetadataManager;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SAMLUserDetailsServiceImpl implements SAMLUserDetailsService {

    @Autowired
    UserRegistryService userRegistryService;

    @Autowired
    private MetadataManager metadata;

    // Logger
    private static final Logger LOG = LoggerFactory.getLogger(SAMLUserDetailsServiceImpl.class);

    public Object loadUserBySAML(SAMLCredential credential)
            throws UsernameNotFoundException {

        String userId = null;

        final String issuer = credential.getAuthenticationAssertion().getIssuer().getValue();

        try {

            ZorroaExtendedMetadata zd = (ZorroaExtendedMetadata) metadata.getExtendedMetadata(issuer);

            /**
             * if the username attr is set, then try to use that, otherwise
             * default to the username that logged in.
             */
            String usernameAttr = trim(zd.getProp("usernameAttr"));
            if (usernameAttr != null) {
                userId = credential.getAttributeAsString(usernameAttr);
            }

            if (userId == null) {
                userId = credential.getNameID().getValue();
            }

            // TODO: make this the ID
            String orgName = credential.getAttributeAsString("organization_name");
            if (orgName != null) {
                LOG.info("Detected organization name from SAML metadata: " +  orgName);
            }

            Map<String, String> attrs = new HashMap();
            for (Attribute a : credential.getAttributes()) {
                attrs.put(a.getName(), credential.getAttributeAsString(a.getName()));
            }

            AuthSource source = new AuthSource(
                    zd.getProp("label"),
                    zd.getProp("authSourceId") + "-saml",
                    zd.getProp("permissionType"),
                    orgName,
                    attrs,
                    parseGroups(zd.getProp("groupAttr"), credential));

            LOG.info("Loading SAML user: {} from {}", userId, issuer);
            return userRegistryService.registerUser(userId, source);

        } catch (Exception e) {
            throw new UsernameNotFoundException("Unable to authenticate user: " + userId, e);
        }
    }

    public List<String> parseGroups(String groupAttrName, SAMLCredential credential) {
        List<String> groups = null;
        if (groupAttrName != null) {
            String[] groupAttr = credential.getAttributeAsStringArray(groupAttrName);
            if (groupAttr != null) {
                groups = Arrays.asList(groupAttr);
            }
        }
        return groups;
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        } else if (value.trim().isEmpty()) {
            return null;
        } else {
            return value.trim();
        }
    }
}
