package com.zorroa.ldap2

import com.zorroa.archivist.sdk.security.AuthSource
import com.zorroa.archivist.sdk.security.UserAuthed
import com.zorroa.archivist.sdk.security.UserRegistryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.ldap.core.DirContextAdapter
import org.springframework.ldap.core.DirContextOperations
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper


open class LdapUserDetailsServiceImpl : LdapAuthoritiesPopulator, UserDetailsContextMapper {

    @Autowired
    private lateinit var userRegistryService: UserRegistryService

    override fun mapUserFromContext(ctx: DirContextOperations, p1: String?, p2: MutableCollection<out GrantedAuthority>?): UserDetails {
        return ctx.getObjectAttribute("user") as UserAuthed
    }

    override fun mapUserToContext(p0: UserDetails?, p1: DirContextAdapter?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * This is called first.
     */
    override fun getGrantedAuthorities(ctx: DirContextOperations, username: String): MutableCollection<out GrantedAuthority> {

         val authed = userRegistryService.registerUser(username,
                 AuthSource("ldap", "ldap", "ldap"), null)

        ctx.setAttributeValue("authorities", authed.authorities)
        ctx.setAttributeValue("user", authed)

        return authed.authorities.toMutableList()
    }
}
