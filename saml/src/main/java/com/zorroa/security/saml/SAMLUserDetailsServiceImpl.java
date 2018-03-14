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

import com.zorroa.archivist.sdk.security.UserAuthed;
import com.zorroa.archivist.sdk.security.UserRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class SAMLUserDetailsServiceImpl implements SAMLUserDetailsService {

	@Autowired
	UserRegistryService userRegistryService;

	// Logger
	private static final Logger LOG = LoggerFactory.getLogger(SAMLUserDetailsServiceImpl.class);

	public Object loadUserBySAML(SAMLCredential credential)
			throws UsernameNotFoundException {

		/**
		 * Grab the username.
		 */
		String userID = credential.getNameID().getValue();
		String issuer = credential.getAuthenticationAssertion().getIssuer().getValue();
		LOG.info("Loading SAML user: {} from {}", userID, issuer);

		/**
		 * Convert the groups attribute to a list of group names.
		 */
		String[] groupAttr = credential.getAttributeAsStringArray("groups");

		List<String> groups = null;
		if (groupAttr != null) {
			groups = Arrays.asList(groupAttr);
		}

		try {
            UserAuthed authed = userRegistryService.registerUser(userID, issuer, groups);
            return authed;

        } catch (Exception e) {
		    LOG.warn("Failed to register user: ", e);
		    throw new UsernameNotFoundException("foo", e);
		}
	}
}
