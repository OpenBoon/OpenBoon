package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Organization
import com.zorroa.archivist.domain.OrganizationSpec
import com.zorroa.archivist.repository.OrganizationDao
import com.zorroa.archivist.security.SuperAdminAuthentication
import com.zorroa.archivist.security.resetAuthentication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.util.*

interface OrganizationService {
    fun create(spec: OrganizationSpec) : Organization
    fun get(id: UUID) : Organization
    fun get(name: String): Organization
    fun getOnlyOne(): Organization
}

@Service
class OrganizationServiceImpl @Autowired constructor (
        val organizationDao: OrganizationDao
) : OrganizationService {

    @Autowired
    internal lateinit var folderService: FolderService

    @Autowired
    internal lateinit var userService: UserService

    @Autowired
    internal lateinit var permissionService: PermissionService

    @Autowired
    internal lateinit var fileStorageService: FileStorageService


    override fun create(spec: OrganizationSpec): Organization {
        val org = organizationDao.create(spec)
        val auth = resetAuthentication(SuperAdminAuthentication(org.id))

        try {
            permissionService.createStandardPermissions(org)
            folderService.createStandardFolders(org)
            userService.createStandardUsers(org)

        } finally {
            resetAuthentication(auth)
        }

        return org
    }

    override fun get(id: UUID): Organization =  organizationDao.get(id)

    override fun get(name: String): Organization =  organizationDao.get(name)

    override fun getOnlyOne(): Organization =  organizationDao.getOnlyOne()



}
