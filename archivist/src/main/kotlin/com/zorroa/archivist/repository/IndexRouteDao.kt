package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.IndexRoute
import com.zorroa.archivist.domain.IndexRouteFilter
import com.zorroa.archivist.domain.IndexRouteSpec
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.repository.KPage
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.JdbcUtils
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

interface IndexRouteDao {

    /**
     * Set a new minor version for the given [IndexRoute]
     */
    fun setMinorVersion(route: IndexRoute, version: Int): Boolean

    /**
     * Set the version that the patch system stopped on as the error version.
     */
    fun setErrorVersion(route: IndexRoute, version: Int): Boolean

    /**
     * Return the [IndexRoute] assigned to the current user's organization.
     */
    fun getOrgRoute(): IndexRoute

    /**
     * Return a random [IndexRoute] that is marked to be in the default pool.
     */
    fun getRandomDefaultRoute(): IndexRoute

    /**
     * Return a list of all [IndexRoute]s, including closed.
     */
    fun getAll(): List<IndexRoute>

    /**
     * Updates all default pool routes to the cluster URL defined in the
     * application.properties file.  This is called once at startup time.
     */
    fun updateDefaultIndexRoutes(clusterUrl: String, useRoutingKey: Boolean)

    /**
     * Return an [IndexRoute] by its unique Id.
     */
    fun get(id: UUID): IndexRoute

    /**
     * Create a new IndexRoute entry.
     */
    fun create(spec: IndexRouteSpec): IndexRoute

    /**
     * Count the number of [IndexRoute]s that match the filter.
     */
    fun count(filter: IndexRouteFilter): Long

    /**
     * Get all [IndexRoute]s that match the filter.
     */
    fun getAll(filter: IndexRouteFilter): KPagedList<IndexRoute>

    /**
     * Find a single [IndexRoute]sthat matches the filter.
     */
    fun findOne(filter: IndexRouteFilter): IndexRoute
}

@Repository
class IndexRouteDaoImpl : AbstractDao(), IndexRouteDao {

    override fun create(spec: IndexRouteSpec): IndexRoute {

        val id = uuid1.generate()
        val time = System.currentTimeMillis()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setString(2, spec.clusterUrl)
            ps.setString(3, spec.indexName)
            ps.setString(4, spec.mapping)
            ps.setInt(5, spec.mappingMajorVer)
            ps.setInt(6, 0)
            ps.setInt(7, spec.replicas)
            ps.setInt(8, spec.shards)
            ps.setBoolean(9, false)
            ps.setBoolean(10, spec.defaultPool)
            ps.setBoolean(11, spec.useRouteKey)
            ps.setLong(12, time)
            ps.setLong(13, time)
            ps.setInt(14, -1)
            ps
        }

        return get(id)
    }

    override fun updateDefaultIndexRoutes(clusterUrl: String, useRoutingKey: Boolean) {
        val count = jdbc.update(
            "UPDATE index_route SET str_url=?,bool_use_rkey=? WHERE pk_index_route=?",
            clusterUrl, useRoutingKey, UUID.fromString("00000000-0000-0000-0000-000000000000")
        )
        logger.info(
            "Updated $count default ES cluster URLs to " +
                "'$clusterUrl', use rkey $useRoutingKey"
        )
    }

    override fun getAll(): List<IndexRoute> {
        return jdbc.query(GET, MAPPER)
    }

    override fun getOrgRoute(): IndexRoute {
        return jdbc.queryForObject(GET_BY_ORG, MAPPER, getOrgId())
    }

    override fun get(id: UUID): IndexRoute {
        return jdbc.queryForObject("$GET WHERE pk_index_route=?", MAPPER, id)
    }

    override fun getRandomDefaultRoute(): IndexRoute {
        return jdbc.query(GET_DEFAULTS, MAPPER).random()
    }

    override fun setMinorVersion(route: IndexRoute, version: Int): Boolean {
        return jdbc.update(UPDATE_MINOR_VER, version, System.currentTimeMillis(), route.id) == 1
    }

    override fun setErrorVersion(route: IndexRoute, version: Int): Boolean {
        return jdbc.update(UPDATE_ERROR_VER, version, System.currentTimeMillis(), route.id) == 1
    }

    override fun count(filter: IndexRouteFilter): Long {
        return jdbc.queryForObject(
            filter.getQuery(COUNT, forCount = true),
            Long::class.java, *filter.getValues(forCount = true)
        )
    }

    override fun getAll(filter: IndexRouteFilter): KPagedList<IndexRoute> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    override fun findOne(filter: IndexRouteFilter): IndexRoute {
        filter.apply { page = KPage(0, 1) }
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return throwWhenNotFound("IndexRoute not found") {
            return KPagedList(1L, filter.page, jdbc.query(query, MAPPER, *values))[0]
        }
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->

            IndexRoute(
                rs.getObject("pk_index_route") as UUID,
                rs.getString("str_url"),
                rs.getString("str_index"),
                rs.getString("str_mapping_type"),
                rs.getInt("int_mapping_major_ver"),
                rs.getInt("int_mapping_minor_ver"),
                rs.getBoolean("bool_closed"),
                rs.getInt("int_replicas"),
                rs.getInt("int_shards"),
                rs.getBoolean("bool_default_pool"),
                rs.getBoolean("bool_use_rkey")
            )
        }

        val INSERT = JdbcUtils.insert(
            "index_route",
            "pk_index_route",
            "str_url",
            "str_index",
            "str_mapping_type",
            "int_mapping_major_ver",
            "int_mapping_minor_ver",
            "int_replicas",
            "int_shards",
            "bool_closed",
            "bool_default_pool",
            "bool_use_rkey",
            "time_created",
            "time_modified",
            "int_mapping_error_ver"
        )

        const val GET = "SELECT * FROM index_route"

        const val COUNT = "SELECT COUNT(1) FROM index_route"

        const val GET_BY_ORG = "$GET " +
            "INNER JOIN " +
            "organization " +
            "ON " +
            "index_route.pk_index_route = organization.pk_index_route " +
            "AND " +
            "organization.pk_organization=?"

        const val GET_DEFAULTS = "$GET WHERE bool_default_pool='t' AND bool_closed='f'"

        const val UPDATE_MINOR_VER =
            "UPDATE " +
                "index_route " +
                "SET " +
                "int_mapping_minor_ver=?, " +
                "int_mapping_error_ver=-1, " +
                "time_modified=? " +
                "WHERE " +
                "pk_index_route=?"

        const val UPDATE_ERROR_VER =
            "UPDATE " +
                "index_route " +
                "SET " +
                "int_mapping_error_ver=?, " +
                "time_modified=? " +
                "WHERE " +
                "pk_index_route=?"
    }
}