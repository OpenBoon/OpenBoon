package boonai.archivist.repository

import boonai.archivist.domain.BoonLib
import boonai.archivist.domain.BoonLibFilter
import boonai.archivist.domain.BoonLibEntity
import boonai.archivist.domain.BoonLibState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BoonLibDao : JpaRepository<BoonLib, UUID>

interface BoonLibJdbcDao {
    fun findOneBoonLib(filter: BoonLibFilter): BoonLib
    fun findAll(filter: BoonLibFilter?): KPagedList<BoonLib>
    fun updateBoonLibState(id: UUID, state: BoonLibState): Boolean
}

@Repository
class BoonLibJdbcDaoImpl : BoonLibJdbcDao, AbstractDao() {

    override fun findOneBoonLib(filter: BoonLibFilter): BoonLib {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return jdbc.queryForObject(query, MAPPER, *values)
    }

    override fun findAll(filter: BoonLibFilter?): KPagedList<BoonLib> {
        val filter = filter ?: BoonLibFilter()
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    private fun count(filter: BoonLibFilter): Long {
        val query = filter.getQuery(COUNT, true)
        return jdbc.queryForObject(query, Long::class.java, *filter.getValues(true))
    }

    override fun updateBoonLibState(id: UUID, state: BoonLibState): Boolean {
        return jdbc.update("UPDATE boonlib SET state=? WHERE pk_boonlib=?", state.ordinal, id) == 1
    }
}

private const val GET = "SELECT * FROM boonlib"

private val MAPPER = RowMapper { rs, _ ->
    BoonLib(
        rs.getObject("pk_boonlib") as UUID,
        rs.getString("name"),
        BoonLibEntity.values()[rs.getInt("entity")],
        rs.getString("entity_type"),
        rs.getString("descr"),
        BoonLibState.values()[rs.getInt("state")],
        rs.getLong("time_created"),
        rs.getLong("time_modified"),
        rs.getString("actor_created"),
        rs.getString("actor_modified")
    )
}

private const val COUNT = "SELECT COUNT(1) FROM boonlib"
