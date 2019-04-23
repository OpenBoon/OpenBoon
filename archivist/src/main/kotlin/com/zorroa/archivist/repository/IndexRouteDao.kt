package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.IndexRoute
import com.zorroa.archivist.security.getOrgId
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface IndexRouteDao {

    /**
     * Set a new minor version for the given [IndexRoute]
     */
    fun setMinorVersion(route: IndexRoute, version: Int) : Boolean

    /**
     * Set the version that the patch system stopped on as the error version.
     */
    fun setErrorVersion(route: IndexRoute, version: Int) : Boolean

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
    fun updateDefaultIndexRoutes(clusterUrl: String)
}

@Repository
class IndexRouteDaoImpl : AbstractDao(), IndexRouteDao {

    override fun updateDefaultIndexRoutes(clusterUrl: String) {
        val count = jdbc.update(
                "UPDATE index_route SET str_url=? WHERE bool_default_pool=?", clusterUrl, true)
        logger.info("Updated $count default ES cluster URLs to '$clusterUrl'")
    }

    override fun getAll(): List<IndexRoute> {
        return jdbc.query(GET, MAPPER)
    }

    override fun getOrgRoute(): IndexRoute {
        return jdbc.queryForObject(GET_BY_ORG, MAPPER, getOrgId())
    }

    override fun getRandomDefaultRoute(): IndexRoute {
        return jdbc.query(GET_DEFAULTS, MAPPER).random()
    }

    override fun setMinorVersion(route: IndexRoute, version: Int) : Boolean {
        return jdbc.update(UPDATE_MINOR_VER, version, System.currentTimeMillis(), route.id) == 1
    }

    override fun setErrorVersion(route: IndexRoute, version: Int) : Boolean {
        return jdbc.update(UPDATE_ERROR_VER, version, System.currentTimeMillis(), route.id) == 1
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->

            IndexRoute(rs.getObject("pk_index_route") as UUID,
                    rs.getString("str_url"),
                    rs.getString("str_index"),
                    rs.getString("str_mapping_type"),
                    rs.getInt("int_mapping_major_ver"),
                    rs.getInt("int_mapping_minor_ver"),
                    rs.getBoolean("bool_closed"),
                    rs.getInt("int_replicas"),
                    rs.getInt("int_shards"),
                    rs.getBoolean("bool_default_pool"),
                    rs.getBoolean("bool_use_rkey"))
        }

        const val GET = "SELECT * FROM index_route"
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