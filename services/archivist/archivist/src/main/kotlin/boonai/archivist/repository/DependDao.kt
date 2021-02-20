package boonai.archivist.repository

import boonai.archivist.domain.Depend
import boonai.archivist.domain.DependSpec
import boonai.archivist.domain.DependState
import boonai.archivist.domain.DependType
import boonai.archivist.security.getZmlpActor
import boonai.archivist.util.JdbcUtils
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

interface DependDao {
    fun create(spec: DependSpec): Depend
    fun incrementDependCount(depend: Depend)
    fun decrementDependCount(depend: Depend)
    fun getWhatDependsOnJob(id: UUID): List<Depend>
    fun getWhatDependsOnTask(id: UUID): List<Depend>
    fun getWhatJobDependsOn(id: UUID): List<Depend>
    fun getWhatTaskDependsOn(id: UUID): List<Depend>
    fun resolve(depends: List<Depend>): Int
    fun get(id: UUID): Depend
}

@Repository
class DependDaoImpl : DependDao, AbstractDao() {

    override fun create(spec: DependSpec): Depend {
        val id = uuid1.generate()
        val time = System.currentTimeMillis()
        val actor = getZmlpActor()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setInt(2, spec.type.ordinal)
            ps.setInt(3, DependState.Active.ordinal)
            ps.setObject(4, spec.dependErJobId)
            ps.setObject(5, spec.dependOnJobId)
            ps.setObject(6, spec.dependErTaskId)
            ps.setObject(7, spec.dependOnTaskId)
            ps.setLong(8, time)
            ps.setLong(9, time)
            ps.setString(10, actor.toString())
            ps.setString(11, actor.toString())
            ps
        }

        return get(id)
    }

    override fun get(id: UUID): Depend {
        return jdbc.queryForObject("$SELECT WHERE pk_depend=?", MAPPER, id)
    }

    override fun incrementDependCount(depend: Depend) {
        when (depend.type) {
            DependType.JobOnJob -> {
                jdbc.update(
                    "UPDATE task SET int_depend_count=int_depend_count+1 WHERE pk_job=?", depend.dependErJobId
                )
            }
            DependType.TaskOnTask -> {
                jdbc.update(
                    "UPDATE task SET int_depend_count=int_depend_count+1 WHERE pk_task=?", depend.dependErTaskId
                )
            }
        }
    }

    override fun decrementDependCount(depend: Depend) {
        when (depend.type) {
            DependType.JobOnJob -> {
                jdbc.update(
                    "UPDATE task SET int_depend_count=int_depend_count-1 WHERE pk_job=?", depend.dependErJobId
                )
            }
            DependType.TaskOnTask -> {
                jdbc.update(
                    "UPDATE task SET int_depend_count=int_depend_count-1 WHERE pk_task=?", depend.dependErTaskId
                )
            }
        }
    }

    override fun getWhatDependsOnJob(id: UUID): List<Depend> {
        return jdbc.query(
            "$SELECT WHERE pk_job_depend_on=? AND int_state=? ORDER BY pk_job_depend_on", MAPPER,
            id, DependState.Active.ordinal
        )
    }

    override fun getWhatDependsOnTask(id: UUID): List<Depend> {
        return jdbc.query(
            "$SELECT WHERE pk_task_depend_on=? AND int_state=? ORDER BY pk_task_depend_on", MAPPER,
            id, DependState.Active.ordinal
        )
    }

    override fun getWhatJobDependsOn(id: UUID): List<Depend> {
        return jdbc.query(
            "$SELECT WHERE pk_job_depend_er=? AND int_state=? ORDER BY pk_job_depend_er", MAPPER,
            id, DependState.Active.ordinal
        )
    }

    override fun getWhatTaskDependsOn(id: UUID): List<Depend> {
        return jdbc.query(
            "$SELECT WHERE pk_task_depend_er=? AND int_state=? ORDER BY pk_task_depend_er", MAPPER,
            id, DependState.Active.ordinal
        )
    }

    override fun resolve(depends: List<Depend>): Int {
        var result = 0
        for (depend in depends) {
            if (jdbc.update(RESOLVE, DependState.Inactive.ordinal, depend.id, DependState.Active.ordinal) == 1) {
                decrementDependCount(depend)
                result += 1
            }
        }
        return result
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->
            Depend(
                rs.getObject("pk_depend") as UUID,
                DependState.values()[rs.getInt("int_state")],
                DependType.values()[rs.getInt("int_type")],
                rs.getObject("pk_job_depend_er") as UUID,
                rs.getObject("pk_job_depend_on") as UUID,
                rs.getObject("pk_task_depend_er") as UUID?,
                rs.getObject("pk_task_depend_on") as UUID?
            )
        }

        private val SELECT = JdbcUtils.select(
            "depend",
            "pk_depend",
            "int_type",
            "int_state",
            "pk_job_depend_er",
            "pk_job_depend_on",
            "pk_task_depend_er",
            "pk_task_depend_on"
        )

        private val INSERT = JdbcUtils.insert(
            "depend",
            "pk_depend",
            "int_type",
            "int_state",
            "pk_job_depend_er",
            "pk_job_depend_on",
            "pk_task_depend_er",
            "pk_task_depend_on",
            "time_created",
            "time_modified",
            "actor_created",
            "actor_modified"
        )

        private val RESOLVE = "UPDATE depend SET int_state=? WHERE pk_depend=? AND int_state=?"
    }
}
