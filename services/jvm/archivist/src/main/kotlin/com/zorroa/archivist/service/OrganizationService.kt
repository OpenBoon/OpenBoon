package com.zorroa.archivist.service

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.IndexRouteSpec
import com.zorroa.archivist.domain.Organization
import com.zorroa.archivist.domain.OrganizationFilter
import com.zorroa.archivist.domain.OrganizationSpec
import com.zorroa.archivist.domain.OrganizationUpdateSpec
import com.zorroa.archivist.repository.IndexRouteDao
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
import java.util.UUID

interface OrganizationService {
    fun create(spec: OrganizationSpec): Organization
    fun get(id: UUID): Organization
    fun get(name: String): Organization
    fun findOne(filter: OrganizationFilter): Organization
    fun getAll(filter: OrganizationFilter): KPagedList<Organization>
    fun update(org: Organization, spec: OrganizationUpdateSpec): Boolean
}

@Service
@Transactional
class OrganizationServiceImpl @Autowired constructor(
    val organizationDao: OrganizationDao,
    val indexRouteDao: IndexRouteDao,
    val indexRoutingService: IndexRoutingService,
    val properties: ApplicationProperties
) : OrganizationService {

    @Autowired
    internal lateinit var fileStorageService: FileStorageService

    override fun create(spec: OrganizationSpec): Organization {

        // will throw if no route is available.
        val ir = indexRouteDao.getRandomPoolRoute()

        if (spec.indexRouteId == null) {
            spec.indexRouteId = ir.id
        }

        val org = organizationDao.create(spec)
        val auth = resetAuthentication(SuperAdminAuthentication(org.id))
        try {
            if (properties.getString("archivist.index.provisioning-method",
                            PROV_DEDICATED) == PROV_DEDICATED) {
                val spec = IndexRouteSpec(
                    ir.clusterUrl,
                    "org_${org.id}_0001",
                    ir.mapping,
                    ir.mappingMajorVer,
                    defaultPool = false
                )
                val orgIndex = indexRoutingService.createIndexRoute(spec)
                organizationDao.update(org, OrganizationUpdateSpec(org.name, orgIndex.id))
            }
        } finally {
            resetAuthentication(auth)
        }

        return org
    }

    override fun update(org: Organization, spec: OrganizationUpdateSpec): Boolean {
        return organizationDao.update(org, spec)
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID): Organization = organizationDao.get(id)

    @Transactional(readOnly = true)
    override fun get(name: String): Organization = organizationDao.get(name)

    @Transactional(readOnly = true)
    override fun findOne(filter: OrganizationFilter): Organization {
        return organizationDao.findOne(filter)
    }

    @Transactional(readOnly = true)
    override fun getAll(filter: OrganizationFilter): KPagedList<Organization> {
        return organizationDao.getAll(filter)
    }

    companion object {

        const val PROV_DEDICATED = "dedicated"
        const val PROV_SHARED = "shared"

        private val logger = LoggerFactory.getLogger(OrganizationServiceImpl::class.java)
    }
}
