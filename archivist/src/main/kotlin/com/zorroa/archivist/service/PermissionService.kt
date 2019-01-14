package com.zorroa.archivist.service

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.collect.ImmutableMap
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.PermissionDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

interface PermissionService {

    fun createStandardPermissions(org: Organization)

    fun getPermissions(): List<Permission>

    fun getPermissions(page: Pager, filter: PermissionFilter): PagedList<Permission>

    fun getPermissions(page: Pager): PagedList<Permission>

    fun getUserAssignablePermissions(page: Pager): PagedList<Permission>

    fun getObjAssignablePermissions(page: Pager): PagedList<Permission>

    fun getPermission(id: UUID): Permission

    fun createPermission(builder: PermissionSpec): Permission

    fun getPermissionNames(): List<String>

    fun permissionExists(authority: String): Boolean

    fun getPermission(name: String): Permission

    fun deletePermission(permission: Permission): Boolean
}

@Service
@Transactional
class PermissionServiceImpl @Autowired constructor(
        private val permissionDao: PermissionDao,
        private val txem: TransactionEventManager
) : PermissionService {

    private val permissionCache = CacheBuilder.newBuilder()
            .maximumSize(200)
            .initialCapacity(100)
            .concurrencyLevel(4)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(object : CacheLoader<String, Permission>() {
                @Throws(Exception::class)
                override fun load(key: String): Permission {
                    return if (key.contains(Permission.JOIN)) {
                        permissionDao.get(key)
                    } else {
                        permissionDao.get(UUID.fromString(key))
                    }
                }
            })


    override fun getPermissions(): List<Permission> {
        return permissionDao.getAll()
    }

    override fun getPermissions(page: Pager): PagedList<Permission> {
        return permissionDao.getPaged(page)
    }

    override fun getPermissions(page: Pager, filter: PermissionFilter): PagedList<Permission> {
        return permissionDao.getPaged(page, filter)
    }

    override fun getUserAssignablePermissions(page: Pager): PagedList<Permission> {
        return permissionDao.getPaged(page,
                PermissionFilter()
                        .setAssignableToUser(true)
                        .forceSort(ImmutableMap.of("str_group", "asc", "str_type", "asc")))
    }

    override fun getObjAssignablePermissions(page: Pager): PagedList<Permission> {
        return permissionDao.getPaged(page,
                PermissionFilter()
                        .setAssignableToObj(true)
                        .forceSort(ImmutableMap.of("str_group", "asc", "str_type", "asc")))
    }

    override fun getPermissionNames(): List<String> {
        return permissionDao.getAll().stream().map({ p -> p.fullName }).collect(Collectors.toList<String>())
    }

    override fun permissionExists(authority: String): Boolean {
        return permissionDao.exists(authority)
    }

    override fun getPermission(name: String): Permission {
        try {
            return permissionCache.get(name)
        } catch (e: Exception) {
            throw EmptyResultDataAccessException("The permission $name does not exist", 1)
        }

    }

    // CACHE
    override fun getPermission(id: UUID): Permission {
        return permissionDao.get(id)
    }

    override fun createPermission(builder: PermissionSpec): Permission {
        val perm = permissionDao.create(builder, false)
        return perm
    }

    val standardPerms = listOf(
            mapOf("name" to  "administrator", "desc" to "Superuser, can do and access everything"),
            mapOf("name" to "everyone", "desc" to "A standard user of the system"),
            mapOf("name" to "share", "desc" to "Modify all permissions"),
            mapOf("name" to "export", "desc" to "Export all files"),
            mapOf("name" to "read", "desc" to "Read all data"),
            mapOf("name" to "write", "desc" to "Write all data"),
            mapOf("name" to "librarian", "desc" to "Manager /library folder"))

    override fun createStandardPermissions(org: Organization) {
        for (entry in standardPerms) {
            val spec = PermissionSpec("zorroa", entry["name"]!!)
            permissionDao.create(spec, true)
        }
    }

    override fun deletePermission(permission: Permission): Boolean {
        return permissionDao.delete(permission)
    }
}
