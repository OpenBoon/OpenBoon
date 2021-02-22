package boonai.archivist.repository

import boonai.archivist.domain.IndexCluster
import boonai.archivist.domain.IndexRoute
import boonai.archivist.domain.IndexRouteFilter
import boonai.archivist.domain.IndexRouteSpec
import boonai.archivist.domain.IndexRouteState
import boonai.archivist.security.getProjectId
import boonai.archivist.util.JdbcUtils
import boonai.archivist.util.randomString
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.common.service.logging.event
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
     * Return the [IndexRoute] assigned to the current user's project.
     */
    fun getProjectRoute(): IndexRoute

    /**
     * Get the project route by project Id.
     */
    fun getProjectRoute(projectId: UUID): IndexRoute

    /**
     * Return a list of all [IndexRoute]s, including closed.
     */
    fun getAll(): List<IndexRoute>

    /**
     * Return a list of all [IndexRoute]s, which are open.
     */
    fun getOpen(): List<IndexRoute>

    /**
     * Return a list of all [IndexRoute]s, which are open by cluster.
     */
    fun getOpen(cluster: IndexCluster): List<IndexRoute>

    /**
     * Return a list of all [IndexRoute]s by cluster.
     */
    fun getAll(cluster: IndexCluster): List<IndexRoute>

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
     * Find a single [IndexRoute] that matches the filter.
     */
    fun findOne(filter: IndexRouteFilter): IndexRoute

    /**
     * Delete an IndexRoute.
     */
    fun delete(route: IndexRoute): Boolean

    /**
     * Set the state of the [IndexRoute].
     */
    fun setState(route: IndexRoute, state: IndexRouteState): Boolean

    fun setState(routeUUID: UUID, state: IndexRouteState): Boolean
}

@Repository
class IndexRouteDaoImpl : AbstractDao(), IndexRouteDao {

    override fun create(spec: IndexRouteSpec): IndexRoute {

        val id = uuid1.generate()
        val time = System.currentTimeMillis()
        val projectId = spec.projectId ?: getProjectId()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, spec.clusterId)
            ps.setObject(3, projectId)
            ps.setInt(4, IndexRouteState.READY.ordinal)
            ps.setString(5, randomString(16).toLowerCase())
            ps.setString(6, spec.mapping)
            ps.setInt(7, spec.majorVer)
            ps.setInt(8, 0)
            ps.setInt(9, spec.replicas)
            ps.setInt(10, spec.shards)
            ps.setLong(11, time)
            ps.setLong(12, time)
            ps.setInt(13, -1)
            ps
        }

        logger.event(
            LogObject.INDEX_ROUTE, LogAction.CREATE,
            mapOf(
                "indexRouteId" to id,
                "overrideProjectId" to projectId
            )
        )
        return get(id)
    }

    override fun getAll(): List<IndexRoute> {
        return jdbc.query(GET, MAPPER)
    }

    override fun getOpen(): List<IndexRoute> {
        return jdbc.query(GET_OPEN, MAPPER)
    }

    override fun getOpen(cluster: IndexCluster): List<IndexRoute> {
        return jdbc.query("$GET_OPEN AND index_cluster.pk_index_cluster=?", MAPPER, cluster.id)
    }

    override fun getAll(cluster: IndexCluster): List<IndexRoute> {
        return jdbc.query("$GET WHERE index_cluster.pk_index_cluster=?", MAPPER, cluster.id)
    }

    override fun getProjectRoute(projectId: UUID): IndexRoute {
        return throwWhenNotFound("Project has no index") {
            return jdbc.queryForObject(
                GET_PROJECT_DEFAULT,
                MAPPER, projectId
            )
        }
    }

    override fun getProjectRoute(): IndexRoute {
        return throwWhenNotFound("Project has no index") {
            return jdbc.queryForObject(
                GET_PROJECT_DEFAULT,
                MAPPER, getProjectId()
            )
        }
    }

    override fun get(id: UUID): IndexRoute {
        return jdbc.queryForObject("$GET WHERE index_route.pk_index_route=?", MAPPER, id)
    }

    override fun setMinorVersion(route: IndexRoute, version: Int): Boolean {
        return jdbc.update(UPDATE_MINOR_VER, version, System.currentTimeMillis(), route.id) == 1
    }

    override fun setErrorVersion(route: IndexRoute, version: Int): Boolean {
        return jdbc.update(UPDATE_ERROR_VER, version, System.currentTimeMillis(), route.id) == 1
    }

    override fun setState(route: IndexRoute, state: IndexRouteState): Boolean {
        return jdbc.update(UPDATE_STATE, state.ordinal, System.currentTimeMillis(), route.id) == 1
    }

    override fun setState(routeUUID: UUID, state: IndexRouteState): Boolean {
        return jdbc.update(UPDATE_STATE, state.ordinal, System.currentTimeMillis(), routeUUID) == 1
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

    override fun delete(route: IndexRoute): Boolean {
        return jdbc.update(
            "DELETE FROM index_route WHERE pk_index_route=?",
            route.id
        ) == 1
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->
            IndexRoute(
                rs.getObject("pk_index_route") as UUID,
                rs.getObject("pk_project") as UUID,
                rs.getObject("pk_index_cluster") as UUID,
                rs.getString("str_url"),
                IndexRouteState.values()[rs.getInt("int_state")],
                rs.getString("str_index"),
                rs.getString("str_mapping_type"),
                rs.getInt("int_mapping_major_ver"),
                rs.getInt("int_mapping_minor_ver"),
                rs.getInt("int_replicas"),
                rs.getInt("int_shards"),
                rs.getString("project_name")
            )
        }

        val INSERT = JdbcUtils.insert(
            "index_route",
            "pk_index_route",
            "pk_index_cluster",
            "pk_project",
            "int_state",
            "str_index",
            "str_mapping_type",
            "int_mapping_major_ver",
            "int_mapping_minor_ver",
            "int_replicas",
            "int_shards",
            "time_created",
            "time_modified",
            "int_mapping_error_ver"
        )

        const val GET = "SELECT index_cluster.str_url, index_route.*, project.str_name as project_name " +
            "FROM " +
            "index_route " +
            "JOIN index_cluster ON (index_route.pk_index_cluster = index_cluster.pk_index_cluster) " +
            "JOIN project ON (index_route.pk_project = project.pk_project) "

        const val GET_OPEN = "$GET WHERE index_route.int_state=0"

        const val GET_PROJECT_DEFAULT = "SELECT index_cluster.str_url, index_route.*, project.str_name as project_name " +
            "FROM " +
            "index_route " +
            "JOIN index_cluster ON (index_route.pk_index_cluster = index_cluster.pk_index_cluster) " +
            "JOIN project ON (index_route.pk_index_route = project.pk_index_route AND project.pk_project=?) "

        const val COUNT = "SELECT COUNT(1) " +
            "FROM " +
            "index_route " +
            "JOIN index_cluster ON (index_route.pk_index_cluster = index_cluster.pk_index_cluster) " +
            "JOIN project ON (index_route.pk_project = project.pk_project) "

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

        const val UPDATE_STATE =
            "UPDATE " +
                "index_route " +
                "SET " +
                "int_state=?, " +
                "time_modified=? " +
                "WHERE " +
                "pk_index_route=?"
    }
}
