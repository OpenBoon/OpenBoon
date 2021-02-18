package boonai.archivist.repository

import com.fasterxml.jackson.module.kotlin.readValue
import boonai.archivist.domain.IndexCluster
import boonai.archivist.domain.IndexClusterFilter
import boonai.archivist.domain.IndexClusterSpec
import boonai.archivist.domain.IndexClusterState
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.archivist.util.JdbcUtils
import boonai.common.service.logging.event
import boonai.common.util.Json
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.lang.IllegalStateException
import java.util.UUID

interface IndexClusterDao {

    /**
     * Creates a new IndexCluster in the pending state.
     */
    fun create(spec: IndexClusterSpec): IndexCluster

    /**
     * Get an [IndexCluster] by its unique UUID.
     */
    fun get(id: UUID): IndexCluster

    /**
     * Get an [IndexCluster] by its unique URL.
     */
    fun get(url: String): IndexCluster

    /**
     * Get all [IndexCluster]s.
     */
    fun getAll(): List<IndexCluster>

    /**
     * Return true if a cluster exists for the URL.
     */
    fun exists(url: String): Boolean

    /**
     * Return an cluster ready to be used by a project.
     */
    fun getNextAutoPoolCluster(): IndexCluster

    /**
     * Update the ES info dump cache from the / endpoint.
     */
    fun updateAttrs(cluster: IndexCluster, json: String): Boolean

    /**
     * Update the state of the cluster to the given state.
     */
    fun updateState(cluster: IndexCluster, state: IndexClusterState): Boolean

    /**
     * Return the number of [IndexCluster]s that match the filter.
     */
    fun count(filter: IndexClusterFilter): Long

    /**
     * Get a page of [IndexCluster]s that match the filter.
     */
    fun getAll(filter: IndexClusterFilter): KPagedList<IndexCluster>

    /**
     * Find a single [IndexCluster]
     */
    fun findOne(filter: IndexClusterFilter): IndexCluster
}

@Repository
class IndexClusterDaoImpl : AbstractDao(), IndexClusterDao {

    override fun create(spec: IndexClusterSpec): IndexCluster {

        val id = uuid1.generate()
        val time = System.currentTimeMillis()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setString(2, spec.url)
            ps.setInt(3, IndexClusterState.PENDING.ordinal)
            ps.setBoolean(4, spec.autoPool)
            ps.setLong(5, time)
            ps.setLong(6, time)
            ps.setInt(7, -1)
            ps
        }

        logger.event(LogObject.INDEX_CLUSTER, LogAction.CREATE, mapOf("indexClusterId" to id))
        return get(id)
    }

    override fun exists(url: String): Boolean {
        return jdbc.queryForObject("$COUNT WHERE str_url=?", Int::class.java, url) > 0
    }

    override fun get(id: UUID): IndexCluster {
        return jdbc.queryForObject("$GET WHERE pk_index_cluster=?", MAPPER, id)
    }

    override fun get(url: String): IndexCluster {
        return jdbc.queryForObject("$GET WHERE str_url=?", MAPPER, url)
    }

    override fun getNextAutoPoolCluster(): IndexCluster {
        val counts = mutableMapOf<String, Int>()
        jdbc.query(GET_POOL_COUNTS) { rs ->
            counts[rs.getString("pk_index_cluster")] = rs.getInt("c")
        }
        if (counts.isEmpty()) {
            throw IllegalStateException("No avaiable ES clusters in auto-pool")
        }
        val sorted = counts.toList()
            .sortedBy { (key, value) -> value }
        return get(UUID.fromString(sorted.last().first))
    }

    override fun getAll(): List<IndexCluster> {
        return jdbc.query(GET, MAPPER)
    }

    override fun updateAttrs(cluster: IndexCluster, json: String): Boolean {
        val time = System.currentTimeMillis()
        return jdbc.update(UPDATE_INFO, time, json, cluster.id) == 1
    }

    override fun updateState(cluster: IndexCluster, state: IndexClusterState): Boolean {
        val time = System.currentTimeMillis()
        return jdbc.update(
            UPDATE_STATE,
            time, state.ordinal, cluster.id, state.ordinal
        ) == 1
    }

    override fun count(filter: IndexClusterFilter): Long {
        return jdbc.queryForObject(
            filter.getQuery(COUNT, forCount = true),
            Long::class.java, *filter.getValues(forCount = true)
        )
    }

    override fun getAll(filter: IndexClusterFilter): KPagedList<IndexCluster> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    override fun findOne(filter: IndexClusterFilter): IndexCluster {
        filter.apply { page = KPage(0, 1) }
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return throwWhenNotFound("IndexCluster not found") {
            return KPagedList(1L, filter.page, jdbc.query(query, MAPPER, *values))[0]
        }
    }
    companion object {

        val MAPPER = RowMapper { rs, _ ->
            IndexCluster(
                rs.getObject("pk_index_cluster") as UUID,
                rs.getString("str_url"),
                rs.getBoolean("bool_autopool"),
                IndexClusterState.values()[rs.getInt("int_state")],
                rs.getLong("time_created"),
                rs.getLong("time_modified"),
                rs.getLong("time_ping"),
                Json.Mapper.readValue(rs.getString("json_attrs"))
            )
        }

        const val GET = "SELECT * FROM index_cluster"

        const val COUNT = "SELECT COUNT(1) FROM index_cluster"

        val INSERT = JdbcUtils.insert(
            "index_cluster",
            "pk_index_cluster",
            "str_url",
            "int_state",
            "bool_autopool",
            "time_created",
            "time_modified",
            "time_ping"
        )

        const val GET_POOL_COUNTS =
            "SELECT " +
                "index_cluster.pk_index_cluster," +
                "COUNT(1) as c " +
                "FROM " +
                "index_cluster LEFT JOIN index_route " +
                "ON (index_cluster.pk_index_cluster = index_route.pk_index_cluster) " +
                "WHERE " +
                "index_cluster.bool_autopool = 't'" +
                "AND " +
                "index_cluster.int_state = 1 " +
                "GROUP BY " +
                "index_cluster.pk_index_cluster"

        const val UPDATE_INFO =
            "UPDATE " +
                "index_cluster " +
                "SET " +
                "time_ping=?," +
                "json_attrs=?::jsonb " +
                "WHERE " +
                "pk_index_cluster=?"

        const val UPDATE_STATE =
            "UPDATE " +
                "index_cluster " +
                "SET " +
                "time_modified=?, " +
                "int_state=? " +
                "WHERE " +
                "pk_index_cluster=? " +
                "AND " +
                "int_state!=?"
    }
}
