package boonai.archivist.repository

import boonai.archivist.domain.BoonLib
import boonai.archivist.domain.BoonLibFilter
import boonai.archivist.domain.BoonLibEntity
import boonai.archivist.domain.BoonLibEntityType
import boonai.archivist.domain.LicenseType
import boonai.archivist.domain.BoonLibState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BoonLibDao : JpaRepository<BoonLib, UUID>

interface BoonLibJdbcDao {
    fun findOneBoonLib(filter: BoonLibFilter): BoonLib
}

@Repository
class BoonLibJdbcDaoImpl : BoonLibJdbcDao, AbstractDao() {

    override fun findOneBoonLib(filter: BoonLibFilter): BoonLib {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return jdbc.queryForObject(query, MAPPER, *values)
    }
}

private const val GET = "SELECT * FROM boonlib"

private val MAPPER = RowMapper { rs, _ ->
    BoonLib(
        rs.getObject("pk_boonlib") as UUID,
        rs.getString("name"),
        BoonLibEntity.values()[rs.getInt("entity")],
        BoonLibEntityType.values()[rs.getInt("entity")],
        rs.getString("descr"),
        LicenseType.values()[rs.getInt("license")],
        BoonLibState.values()[rs.getInt("state")],
        rs.getLong("time_created"),
        rs.getLong("time_modified"),
        rs.getString("actor_created"),
        rs.getString("actor_modified")
    )
}
