package com.zorroa.archivist.security

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Access
import com.zorroa.archivist.domain.Document
import com.zorroa.common.schema.PermissionSchema
import com.zorroa.security.Groups
import org.elasticsearch.index.query.QueryStringQueryBuilder
import org.elasticsearch.index.query.TermsQueryBuilder
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccessResolverTests : AbstractTest() {

    @Autowired
    lateinit var accessResolver: AccessResolver

    @Test
    fun testGetAssetPermissionsFilter() {
        authenticate("user")
        //  read
        var filter = accessResolver.getAssetPermissionsFilter(Access.Read) as TermsQueryBuilder
        assertEquals("system.permissions.read", filter.fieldName())

        filter = accessResolver.getAssetPermissionsFilter(Access.Delete) as TermsQueryBuilder
        assertEquals("system.permissions.delete", filter.fieldName())

        filter = accessResolver.getAssetPermissionsFilter(Access.Write) as TermsQueryBuilder
        assertEquals("system.permissions.write", filter.fieldName())

        filter = accessResolver.getAssetPermissionsFilter(Access.Export) as TermsQueryBuilder
        assertEquals("system.permissions.export", filter.fieldName())
    }

    @Test
    fun testHasAccess() {
        authenticate("user", perms = listOf(Groups.EVERYONE))
        val everyone = permissionService.getPermission(Groups.EVERYONE)
        val perms = PermissionSchema().apply {
            addToRead(everyone.id)
        }

        val doc = Document()
        doc.setAttr("system.permissions", perms)
        assertTrue(accessResolver.hasAccess(Access.Read, doc))
    }

    @Test
    fun testHasAccessFailure() {
        authenticate("user", perms = listOf(Groups.EVERYONE))
        val everyone = permissionService.getPermission(Groups.ADMIN)
        val perms = PermissionSchema().apply {
            addToRead(everyone.id)
        }

        val doc = Document()
        doc.setAttr("system.permissions", perms)
        assertFalse(accessResolver.hasAccess(Access.Read, doc))
    }
}

@TestPropertySource(locations = ["classpath:test.properties", "classpath:jwt.properties"])
class JwtAccessResolverTests : AbstractTest() {

    @Autowired
    lateinit var accessResolver: AccessResolver

    @Test
    fun testGetAssetPermissionsFilter() {
        authenticate("user", qStringFilter = "source.path:*", perms = listOf(Groups.READ))
        val filter = accessResolver.getAssetPermissionsFilter(Access.Read) as QueryStringQueryBuilder
        assertEquals("source.path:*", filter.queryString())
    }

    @Test(expected = AccessDeniedException::class)
    fun testGetAssetPermissionsFilterFailure() {
        authenticate("user", qStringFilter = "source.path:*", perms = listOf(Groups.WRITE))
        accessResolver.getAssetPermissionsFilter(Access.Read) as QueryStringQueryBuilder
    }

    @Test
    fun testHasAccess() {
        authenticate("user", perms = listOf(Groups.READ))
        assertTrue(accessResolver.hasAccess(Access.Read, Document()))
    }

    @Test
    fun testHasAccessFailure() {
        authenticate("user", perms = listOf(Groups.WRITE))
        assertFalse(accessResolver.hasAccess(Access.Read, Document()))
    }
}