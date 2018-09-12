package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.OrganizationSpec
import com.zorroa.archivist.security.SuperAdminAuthentication
import org.junit.Test
import kotlin.test.assertEquals
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull


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

        val libFolder = folderService.get("/Library")
        assertEquals(org.id, userFolder!!.organizationId)
        assertFalse(libFolder!!.recursive)
        assertNotNull(libFolder)
    }

    @Test
    fun testGet() {
        val org = organizationService.create(organizationSpec)
        assertEquals(org, organizationService.get(org.id))
    }

    @Test
    fun testGetOnlyOne() {
        organizationService.getOnlyOne()
    }
}
