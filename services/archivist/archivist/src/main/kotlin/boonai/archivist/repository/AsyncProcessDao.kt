package boonai.archivist.repository

import boonai.archivist.domain.AsyncProcess
import boonai.archivist.domain.AsyncProcessState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AsyncProcessDao : JpaRepository<AsyncProcess, UUID> {
    fun findTopByStateOrderByTimeCreatedAsc(state: AsyncProcessState): AsyncProcess?
    fun findByStateAndTimeRefreshLessThan(state: AsyncProcessState, time: Long): List<AsyncProcess>

    @Modifying
    @Query("UPDATE AsyncProcess a SET a.timeRefresh = ?1 WHERE a.id = ?2")
    fun updateRefreshTime(time: Long, id: UUID): Int
}

interface AsyncProcessJdbcDao {
    fun setState(proc: AsyncProcess, newState: AsyncProcessState, oldState: AsyncProcessState): Boolean
    fun setState(proc: AsyncProcess, newState: AsyncProcessState): Boolean
}

@Repository
class AsyncProcessJdbcDaoImpl : AsyncProcessJdbcDao, AbstractDao() {

    override fun setState(proc: AsyncProcess, newState: AsyncProcessState, oldState: AsyncProcessState): Boolean {
        return jdbc.update(
            "UPDATE process SET int_state=? WHERE pk_process=? AND int_state=?",
            newState.ordinal, proc.id, oldState.ordinal
        ) == 1
    }

    override fun setState(proc: AsyncProcess, newState: AsyncProcessState): Boolean {
        return jdbc.update(
            "UPDATE process SET int_state=? WHERE pk_process=? AND int_state!=?",
            newState.ordinal, proc.id, newState.ordinal
        ) == 1
    }
}
