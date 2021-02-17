package boonai.archivist.service

import boonai.archivist.domain.Depend
import boonai.archivist.domain.DependSpec
import boonai.archivist.domain.DependType
import boonai.archivist.domain.InternalTask
import boonai.archivist.domain.JobId
import boonai.archivist.domain.Task
import boonai.archivist.repository.DependDao
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.common.service.logging.event
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface DependService {
    fun createDepend(spec: DependSpec): Depend
    fun createDepend(dependOnTask: Task, dependErTasks: List<Task>)
    fun createDepend(dependErJobId: JobId, dependOnJobIds: List<UUID>)
    fun getDepend(id: UUID): Depend
    fun resolveDependsOnJob(job: JobId): Int
    fun resolveDependsOnTask(task: InternalTask): Int
    fun dropDepends(job: JobId): Int
    fun dropDepends(task: InternalTask): Int
}

@Service
@Transactional
class DependServiceImpl(val dependDao: DependDao) : DependService {

    override fun createDepend(spec: DependSpec): Depend {
        val depend = dependDao.create(spec)
        dependDao.incrementDependCount(depend)

        logger.event(
            LogObject.DEPEND, LogAction.CREATE,
            mapOf(
                "dependId" to depend.id,
                "dependType" to spec.type.name
            )
        )
        return depend
    }

    override fun createDepend(dependErJobId: JobId, dependOnJobIds: List<UUID>) {
        for (dependOnJobId in dependOnJobIds) {
            val spec = DependSpec(
                DependType.JobOnJob,
                dependErJobId.jobId,
                dependOnJobId,
                null, null
            )
            createDepend(spec)
        }
    }

    override fun createDepend(dependOnTask: Task, dependErTasks: List<Task>) {
        for (dependErTask in dependErTasks) {
            val spec = DependSpec(
                DependType.TaskOnTask,
                dependErTask.jobId,
                dependOnTask.jobId,
                dependErTask.id,
                dependOnTask.id
            )
            createDepend(spec)
        }
    }

    override fun getDepend(id: UUID): Depend {
        return dependDao.get(id)
    }

    override fun resolveDependsOnJob(job: JobId): Int {
        val depends = dependDao.getWhatDependsOnJob(job.jobId)
        if (depends.isNotEmpty()) {
            logger.event(
                LogObject.DEPEND, LogAction.RESOLVE,
                mapOf(
                    "dependType" to "JobOnJob",
                    "jobId" to job.jobId
                )
            )
            return dependDao.resolve(depends)
        }

        return 0
    }

    override fun resolveDependsOnTask(task: InternalTask): Int {
        val depends = dependDao.getWhatDependsOnTask(task.taskId)
        if (depends.isNotEmpty()) {
            logger.event(
                LogObject.DEPEND, LogAction.RESOLVE,
                mapOf(
                    "dependType" to "TaskOnTask",
                    "taskId" to task.taskId
                )
            )
            return dependDao.resolve(depends)
        }

        return 0
    }

    override fun dropDepends(job: JobId): Int {

        val depends = dependDao.getWhatJobDependsOn(job.jobId)
        if (depends.isNotEmpty()) {
            logger.event(
                LogObject.DEPEND, LogAction.RESOLVE,
                mapOf(
                    "dependType" to "JobOnJob",
                    "jobId" to job.jobId
                )
            )
            return dependDao.resolve(depends)
        }

        return 0
    }

    override fun dropDepends(task: InternalTask): Int {

        val depends = dependDao.getWhatTaskDependsOn(task.taskId)
        if (depends.isNotEmpty()) {
            logger.event(
                LogObject.DEPEND, LogAction.RESOLVE,
                mapOf(
                    "dependType" to "TaskOnTask",
                    "taskId" to task.taskId
                )
            )
            return dependDao.resolve(depends)
        }

        return 0
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DependServiceImpl::class.java)
    }
}
