package com.zorroa.archivist.sdk.security

interface LdapUserDetailsPlugin {

    val groupType: String

    val emailDomain: String

    fun getGroups(username: String): List<String>

}
