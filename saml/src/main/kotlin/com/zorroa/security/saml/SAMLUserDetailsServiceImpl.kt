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

package com.zorroa.security.saml

import com.zorroa.archivist.sdk.security.AuthSource
import com.zorroa.archivist.sdk.security.UserRegistryService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.saml.SAMLCredential
import org.springframework.security.saml.metadata.MetadataManager
import org.springframework.security.saml.userdetails.SAMLUserDetailsService
import org.springframework.stereotype.Service

@Service
class SAMLUserDetailsServiceImpl : SAMLUserDetailsService {

    @Autowired
    lateinit var userRegistryService: UserRegistryService

    @Autowired
    lateinit var metadata: MetadataManager

    @Throws(UsernameNotFoundException::class)
    override fun loadUserBySAML(credential: SAMLCredential): Any {

        var userId: String? = null

        val issuer = credential.authenticationAssertion.issuer.value

        try {

            val zd = metadata.getExtendedMetadata(issuer) as ZorroaExtendedMetadata

            /**
             * if the username attr is set, then try to use that, otherwise
             * default to the username that logged in.
             */
            val usernameAttr = trim(zd.props.getProperty("usernameAttr"))
            if (usernameAttr != null) {
                userId = credential.getAttributeAsString(usernameAttr)
            }

            if (userId == null) {
                userId = credential.nameID.value
            }

            // TODO: make this the ID
            val orgPrefix = zd.props.getProperty("organizationPrefix")
            val orgId = credential.getAttributeAsString(zd.props.getProperty("organizationAttr"))
            val orgName: String

            orgName = if (orgId != null) {
                orgPrefix + orgId
            } else {
                "Zorroa"
            }

            LOG.info("Detected organization name from SAML metadata: $orgName")

            val attrs = mutableMapOf<String, String>()
            for (a in credential.attributes) {
                attrs[a.name] = credential.getAttributeAsString(a.name)
            }

            val source = AuthSource(
                    zd.props.getProperty("label"),
                    zd.props.getProperty("authSourceId") + "-saml",
                    zd.props.getProperty("permissionType"),
                    orgName,
                    attrs,
                    parseGroups(zd.props.getProperty("groupAttr"), credential))

            LOG.info("Loading SAML user: {} from {}", userId, issuer)
            return userRegistryService.registerUser(userId!!, source)

        } catch (e: Exception) {
            throw UsernameNotFoundException("Unable to authenticate user: " + userId!!, e)
        }

    }

    fun parseGroups(groupAttrName: String?, credential: SAMLCredential): List<String> {
        LOG.info("Loading SAML group attribute {}", groupAttrName)
        val groups = mutableListOf<String>()
        if (groupAttrName != null) {
            val groupAttrArray = credential.getAttributeAsStringArray(groupAttrName)
            LOG.info("Loading SAML group attribute {}", groupAttrArray)

            if (groupAttrArray != null) {
                if (groupAttrArray.isNotEmpty()) {
                    LOG.info("Found Group array with length {}", groupAttrArray.size)
                    groups.addAll(groupAttrArray)
                }
            } else {
                val groupAttrString = credential.getAttributeAsString(groupAttrName)
                if (groupAttrString != null) {
                    LOG.info("Found Group string {}", groupAttrString)
                    groups.add(groupAttrString)
                }
            }
        }
        return groups
    }

    companion object {

        // Logger
        private val LOG = LoggerFactory.getLogger(SAMLUserDetailsServiceImpl::class.java)

        private fun trim(value: String?): String? {

            return if (value == null) {
                null
            } else if (value.trim { it <= ' ' }.isEmpty()) {
                null
            } else {
                value.trim { it <= ' ' }
            }
        }
    }
}
