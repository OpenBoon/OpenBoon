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
import com.zorroa.archivist.sdk.security.UserAuthed;
import com.zorroa.archivist.sdk.security.UserRegistryService;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.metadata.MetadataManager;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

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

		/**
		 * Grab the username.
		 */

		String userID = credential.getNameID().getValue();
		String issuer = credential.getAuthenticationAssertion().getIssuer().getValue();

		try {
			ZorroaExtendedMetadata zd = (ZorroaExtendedMetadata) metadata.getExtendedMetadata(issuer);
			AuthSource source = new AuthSource(
					zd.getProp("label"),
					zd.getProp("authSourceId"),
					zd.getProp("permissionType"));

			LOG.info("Loading SAML user: {} from {}", userID, issuer);

			/**
			 * Convert the groups attribute to a list of group names.
			 */
			List<String> groups = null;
			String groupAttrName = zd.getProp("groupAttr");
			if (groupAttrName != null) {
				String[] groupAttr = credential.getAttributeAsStringArray(groupAttrName);
				if (groupAttr != null) {
					groups = Arrays.asList(groupAttr);
				}
			}

			try {
				UserAuthed authed = userRegistryService.registerUser(userID, source, groups);
				return authed;

			} catch (Exception e) {
				LOG.warn("Failed to register user: ", e);
				throw new UsernameNotFoundException("foo", e);
			}

		} catch (MetadataProviderException e) {
			LOG.warn("Failed to register user, IDP not founds", e);
			throw new UsernameNotFoundException("Unable to find IDP associated with user");
		}
	}
}
