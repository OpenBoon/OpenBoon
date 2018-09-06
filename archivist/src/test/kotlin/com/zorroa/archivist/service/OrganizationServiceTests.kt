package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.OrganizationSpec
import org.junit.Test
import kotlin.test.assertEquals


class OrganizationServiceTests : AbstractTest() {

    private val organizationName = "Wendys"
    private val organizationSpec = OrganizationSpec(organizationName)

    @Test
    fun testCreateWithSpec() {
        val org = organizationService.create(organizationSpec)
        assertEquals(organizationName, org.name)
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
