package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.DataSource
import com.zorroa.archivist.domain.DataSourceCredentials
import com.zorroa.archivist.security.getProjectId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

interface DataSourceDao : JpaRepository<DataSource, UUID> {

    fun getByName(name: String): DataSource
}

interface DataSourceJdbcDao {

    /**
     * Update a [DataSource] credentials blob. Setting the blob to null
     * will remove it.  The blob must be encrypted before calling this.
     */
    fun updateCredentials(id: UUID, creds: String?, salt: String) : Boolean

    /**
     * Get an encrypted [DataSourceCredentials] blob.
     */
    fun getCredentials(id: UUID) : DataSourceCredentials
}


@Repository
class JdbcDataSourceJdbcDaoImpl : AbstractDao(), DataSourceJdbcDao {

    override fun updateCredentials(id: UUID, creds: String?, salt: String) : Boolean {
        return jdbc.update("UPDATE datasource SET str_credentials=?, str_salt=? WHERE pk_datasource=? AND pk_project=?",
            creds, salt, id, getProjectId()) == 1
    }

    override fun getCredentials(id: UUID) : DataSourceCredentials {
        return jdbc.queryForObject(GET,
            RowMapper { rs, _ ->
            DataSourceCredentials(
                rs.getString("str_credentials"),
                rs.getString("str_salt"))
        }, id, getProjectId())
    }

    companion object {
        const val GET = "SELECT " +
                "str_credentials, " +
                "str_salt " +
            "FROM " +
                "datasource " +
            "WHERE " +
                "pk_datasource=? " +
            "AND " +
                "pk_project=? "
    }
}