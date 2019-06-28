package com.zorroa.ldap

import com.zorroa.archivist.sdk.security.LdapUserDetailsPlugin

open class StandardLdapUserDetailsPlugin : LdapUserDetailsPlugin {

    override val groupType: String
        get() = "ldap"

    override val emailDomain: String
        get() = "zorroa.com"

    override fun getGroups(username: String): List<String> {
        return listOf()
    }

}