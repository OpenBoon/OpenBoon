package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Organization
import com.zorroa.archivist.domain.OrganizationSpec
import com.zorroa.archivist.repository.OrganizationDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

interface OrganizationService {
    fun create(name: String) : Organization
    fun create(spec: OrganizationSpec) : Organization
    fun get(id: UUID) : Organization
}

@Service
class OrganizationServiceImpl @Autowired constructor () : OrganizationService {

    @Autowired
    private lateinit var organizationDao: OrganizationDao

    override fun create(name: String): Organization {
        val spec = OrganizationSpec(name)
        return organizationDao.create(spec)
    }

    override fun create(spec: OrganizationSpec): Organization {
        return organizationDao.create(spec)
    }

    override fun get(id: UUID) = organizationDao.get(id)
}