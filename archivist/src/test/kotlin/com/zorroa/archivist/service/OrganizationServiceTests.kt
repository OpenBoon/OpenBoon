package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Organization
import com.zorroa.archivist.domain.OrganizationSpec
import com.zorroa.archivist.repository.OrganizationDao
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import org.mockito.Mockito
import java.util.*


@RunWith(MockitoJUnitRunner::class)
class OrganizationServiceTests {

    private val organizationName = "Wendys"
    private val organizationSpec = OrganizationSpec(organizationName)
    private val organizationId = UUID.randomUUID()
    private val organization = Organization(organizationId.toString(), organizationName)

    @InjectMocks
    private lateinit var organizationService: OrganizationServiceImpl

    @Mock
    lateinit var organizationDao: OrganizationDao

    @Before
    fun init() {
        Mockito.`when`(organizationDao.create(organizationSpec)).thenReturn(organization)
        Mockito.`when`(organizationDao.get(organizationId)).thenReturn(organization)
    }

    @Test
    fun testCreateWithString() {
        assertEquals(organization, organizationService.create("Wendys"))
    }

    @Test
    fun testCreateWithSpec() {
        assertEquals(organization, organizationService.create(organizationSpec))
    }

    @Test
    fun testGet() {
        assertEquals(organization, organizationService.get(organizationId))
    }
}