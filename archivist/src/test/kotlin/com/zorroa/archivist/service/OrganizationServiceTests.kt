package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.OrganizationSpec
import com.zorroa.archivist.security.SuperAdminAuthentication
import org.junit.Test
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.*


class OrganizationServiceTests : AbstractTest() {

    private val organizationName = "Wendys"
    private val organizationSpec = OrganizationSpec(organizationName)

    @Test
    fun testCreateWithSpec() {
        val org = organizationService.create(organizationSpec)
        assertEquals(organizationName, org.name)
        SecurityContextHolder.getContext().authentication = SuperAdminAuthentication(org.id)

        val userFolder = folderService.get("/Users")
        assertEquals(org.id, userFolder!!.organizationId)
        assertFalse(userFolder!!.recursive)
        assertNull(userFolder!!.search)

        assertEquals(1, userFolder.acl?.size)
        assertEquals("zorroa::everyone", userFolder.acl!![0].permission)

        val libFolder = folderService.get("/Library")
        assertEquals(org.id, userFolder!!.organizationId)
        assertFalse(libFolder!!.recursive)
        assertNotNull(libFolder)

        assertEquals(2, libFolder.acl?.size)
        val perms = libFolder.acl?.map {
            it.permission
        }
        assertTrue(perms!!.contains("zorroa::librarian"))
        assertTrue(perms!!.contains("zorroa::everyone"))

    }

    @Test
    fun testGet() {
        val org = organizationService.create(organizationSpec)
        assertEquals(org.id, organizationService.get(org.id).id)
    }

}
