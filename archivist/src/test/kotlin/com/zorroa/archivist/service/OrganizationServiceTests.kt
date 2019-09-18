package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Folder
import com.zorroa.archivist.domain.OrganizationFilter
import com.zorroa.archivist.domain.OrganizationSpec
import com.zorroa.archivist.security.SuperAdminAuthentication
import com.zorroa.security.Groups
import org.junit.Test
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrganizationServiceTests : AbstractTest() {

    private val organizationName = "UUS Enterprise"
    private val organizationSpec = OrganizationSpec(organizationName)

    @Test(expected = IllegalStateException::class)
    fun testCreateMaxOrganizationsPerIndex() {
        val max = properties.getInt("archivist.index.provisioning-max")
        for (i in 0..max + 1) {
            val ospec = OrganizationSpec("org$i")
            organizationService.create(ospec)
        }

        for (org in organizationService.getAll(OrganizationFilter())) {
            logger.info("${org.indexRouteId}")
        }
    }

    @Test
    fun testCreateWithSpec() {
        val org = organizationService.create(organizationSpec)
        assertEquals(organizationName, org.name)
        SecurityContextHolder.getContext().authentication = SuperAdminAuthentication(org.id)

        val userFolder = folderService.get("/Users") as Folder
        assertEquals(org.id, userFolder.organizationId)
        assertFalse(userFolder.recursive)
        assertNull(userFolder.search)

        assertEquals(1, userFolder.acl?.size)
        assertEquals(Groups.EVERYONE, userFolder.acl?.get(0)!!.permission)

        val libFolder = folderService.get("/Library") as Folder
        assertEquals(org.id, userFolder.organizationId)
        assertFalse(libFolder.recursive)
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
