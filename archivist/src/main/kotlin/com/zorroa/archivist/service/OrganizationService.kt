package com.zorroa.archivist.service

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.Organization
import com.zorroa.archivist.domain.OrganizationFilter
import com.zorroa.archivist.domain.OrganizationSpec
import com.zorroa.archivist.domain.OrganizationUpdateSpec
import com.zorroa.archivist.repository.OrganizationDao
import com.zorroa.archivist.security.SuperAdminAuthentication
import com.zorroa.archivist.security.resetAuthentication
import com.zorroa.common.repository.KPagedList
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface OrganizationService {
    fun create(spec: OrganizationSpec) : Organization
    fun get(id: UUID) : Organization
    fun get(name: String): Organization
    fun findOne(filter: OrganizationFilter): Organization
    fun getAll(filter: OrganizationFilter): KPagedList<Organization>
    fun update(org: Organization, spec: OrganizationUpdateSpec): Boolean
}

@Service
@Transactional
class OrganizationServiceImpl @Autowired constructor (
        val organizationDao: OrganizationDao,
        val properties: ApplicationProperties
) : OrganizationService, ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    internal lateinit var folderService: FolderService

    @Autowired
    internal lateinit var userService: UserService

    @Autowired
    internal lateinit var permissionService: PermissionService

    @Autowired
    internal lateinit var fileStorageService: FileStorageService

    @Autowired
    internal lateinit var fieldSystemService: FieldSystemService

    override fun create(spec: OrganizationSpec): Organization {
        val org = organizationDao.create(spec)
        val auth = resetAuthentication(SuperAdminAuthentication(org.id))

        try {
            permissionService.createStandardPermissions(org)
            folderService.createStandardFolders(org)
            userService.createStandardUsers(org)
            fieldSystemService.setupDefaultFieldSets(org)

        } finally {
            resetAuthentication(auth)
        }

        return org
    }

    override fun update(org: Organization, spec: OrganizationUpdateSpec): Boolean {
        return organizationDao.update(org, spec)
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID): Organization =  organizationDao.get(id)

    @Transactional(readOnly = true)
    override fun get(name: String): Organization =  organizationDao.get(name)

    @Transactional(readOnly = true)
    override fun findOne(filter: OrganizationFilter): Organization  {
        return organizationDao.findOne(filter)
    }

    @Transactional(readOnly = true)
    override fun getAll(filter: OrganizationFilter): KPagedList<Organization> {
        return organizationDao.getAll(filter)
    }

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        if (!properties.getBoolean("unittest", false)) {
            createDefaultOrganizationFieldSets()
        }
    }

    // Only called once at startup if there are no field sets.
    fun createDefaultOrganizationFieldSets() {
        val org = Organization(UUID.fromString("00000000-9998-8888-7777-666666666666"), "Zorroa")
        val auth = resetAuthentication(SuperAdminAuthentication(org.id))
        try {
            if (fieldSystemService.getAllFieldSets().isEmpty()) {
                fieldSystemService.setupDefaultFieldSets(org)
            }
        } finally {
            resetAuthentication(auth)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OrganizationServiceImpl::class.java)
    }
}
