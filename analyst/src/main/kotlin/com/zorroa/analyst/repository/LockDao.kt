package com.zorroa.analyst.repository

import com.zorroa.analyst.domain.Lock
import com.zorroa.analyst.domain.LockSpec
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface LockDao {
    fun create(spec: LockSpec) : Lock
    fun get(id: UUID) : Lock
    fun getByAsset(assetId: UUID) : Lock
    fun deleteByJob(jobId: UUID) : Int
}

@Repository
class LockDaoImpl : AbstractJdbcDao(), LockDao {

    override fun create(spec: LockSpec) : Lock {
        val id = uuid1.generate()
        val time = System.currentTimeMillis()

        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, spec.assetId)
            ps.setObject(3, spec.jobId)
            ps.setLong(4, time)
            ps
        })

        return Lock(id, spec.assetId, spec.jobId)
    }

    override fun get(id: UUID) : Lock {
        return jdbc.queryForObject("$GET WHERE pk_lock=?", MAPPER, id)
    }

    override fun getByAsset(assetId: UUID) : Lock {
        return jdbc.queryForObject("$GET WHERE pk_asset=?", MAPPER, assetId)
    }

    override fun deleteByJob(jobId: UUID) : Int {
        return jdbc.update("DELETE FROM lock WHERE pk_job=?", jobId)
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->
            Lock(rs.getObject("pk_lock") as UUID,
                    rs.getObject("pk_asset") as UUID,
                    rs.getObject("pk_job") as UUID)
        }

        private val GET = "SELECT * FROM lock"

        private val INSERT = sqlInsert("lock",
                "pk_lock",
                "pk_asset",
                "pk_job",
                "time_created")
    }

}
