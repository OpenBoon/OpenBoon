package com.zorroa.archivist.web

import com.zorroa.archivist.domain.Organization
import com.zorroa.archivist.domain.OrganizationSpec
import com.zorroa.archivist.repository.OrganizationDao
import com.zorroa.archivist.service.OrganizationServiceImpl
import com.zorroa.archivist.web.api.OrganizationController
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class OrganizationControllerTests {

    private val organizationName = "Wendys"
    private val organizationSpec = OrganizationSpec(organizationName)
    private val organization = Organization(UUID.randomUUID().toString(), organizationName)

    @InjectMocks
    private lateinit var organizationService: OrganizationServiceImpl

    @Mock
    lateinit var organizationDao: OrganizationDao

    @Before
    fun init() {
        Mockito.`when`(organizationDao.create(organizationSpec)).thenReturn(organization)
    }

    @Test
    fun testCreate() {
        val organizationController = OrganizationController(organizationService)
        val organization = organizationController.create(organizationSpec)
        assertEquals(organizationName, organization.name)
    }

}