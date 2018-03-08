package com.zorroa.archivist.service

import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.JobDao
import com.zorroa.archivist.repository.PipelineDao
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.archivist.security.getUsername
import com.zorroa.cluster.thrift.ExpandT
import com.zorroa.common.config.ApplicationProperties
import com.zorroa.common.config.NetworkEnvironment
import com.zorroa.common.domain.TaskState
import com.zorroa.sdk.client.exception.ArchivistException
import com.zorroa.sdk.client.exception.ArchivistWriteException
import com.zorroa.sdk.domain.Document
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.processor.Expand
import com.zorroa.sdk.processor.PipelineType
import com.zorroa.sdk.processor.ProcessorRef
import com.zorroa.sdk.util.Json
import com.zorroa.sdk.zps.ZpsScript
import org.apache.commons.lang3.StringUtils
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Created by chambers on 6/24/16.
 */
interface JobService {

    fun launch(spec: JobSpecV): Job

    /**
     * Launches a Job using the given ZPS script. Returns the script
     * back populated with the jobId and first task Id.
     *
     * @param spec
     * @return
     */
    fun launch(spec: JobSpec): Job

    fun createParentDepend(task: TaskId): Boolean

    fun expand(parent: Task, expand: ExpandT): List<Task>

    /**
     * Create a new task with a validated TaskSpecV.  The validated spec
     * can only reference a pipeline.  Currently this is only called
     * by CloudProxy
     *
     * @param spec
     * @return
     */
    fun continueImportTask(spec: TaskSpecV): Task

    /**
     * Create a new task.
     *
     * @param script
     * @return
     */
    fun createTask(spec: TaskSpec): Task

    /**
     * Get a job by id.
     *
     * @param id
     * @return
     */
    operator fun get(id: Int): Job

    /**
     * Set the state of a given job.
     *
     * @param job
     * @param newState
     * @param oldState
     * @return
     */
    fun setJobState(job: JobId, newState: JobState, oldState: JobState): Boolean

    /**
     * Set the state of a given task.  The current state must be the expected state.
     *
     * @param task
     * @param newState
     * @param expect
     * @return
     */
    fun setTaskState(task: TaskId, newState: TaskState, vararg expect: TaskState): Boolean

    /**
     * Set the host the task is running on.
     *
     * @param task
     * @param host
     */
    fun setHost(task: TaskId, host: String)

    /**
     * Set the task state to queued.
     *
     * @return
     */
    fun setTaskQueued(id: TaskId): Boolean

    fun setTaskQueued(script: TaskId, host: String): Boolean

    fun setTaskCompleted(task: Task, exitStatus: Int): Boolean

    fun getTasks(jobId: Int, state: TaskState): List<Task>

    fun setTaskRunning(task: Task): Boolean

    fun incrementStats(task: TaskId, adder: TaskStatsAdder)

    fun decrementStats(task: Task)

    /**
     * Increment asset related stats.
     *
     * @param id
     * @param addr
     * @return
     */
    //boolean incrementJobStats(int id, TaskStatsAdder addr);

    //boolean incrementTaskStats(int id, TaskStatsAdder addr);

    /**
     * Return a list of jobs matching the given criteria.
     *
     * @param page
     * @param filter
     * @return
     */
    fun getAll(page: Pager, filter: JobFilter): PagedList<Job>

    fun getAllTasks(job: Int, page: Pager): PagedList<Task>

    fun getAllTasks(job: Int, page: Pager, filter: TaskFilter): PagedList<Task>

    fun updatePingTime(taskIds: List<Int>): Int

    fun resolveJobRoot(spec: JobSpec): Path
}

@Service
@Transactional
class JobServiceImpl @Autowired constructor(
        val jobDao: JobDao,
        val taskDao: TaskDao,
        val pipelineDao: PipelineDao,
        val properties: ApplicationProperties,
        val networkEnv: NetworkEnvironment
): JobService {

    @Autowired
    private lateinit var pipelineService : PipelineService

    @Autowired
    private lateinit var pluginService : PluginService

    /**
     * The subdirectories made in a job directory.
     *
     * logs and tmp get removed by maintenance.
     * if assets has files its left alone
     */
    private val CHILD_DIRS = arrayOf("logs", "tmp", "assets", "exported", "scripts")

    /**
     * Launch a validated JobSpec provided by REST endpoint.
     * @param spec
     * @return
     */
    override fun launch(spec: JobSpecV): Job {

        if (!JdbcUtils.isValid(spec.script.generate) && !JdbcUtils.isValid(spec.script.over)) {
            // Add 1 empty frame.
            spec.script.over = Lists.newArrayList(Document())
            //throw new IllegalArgumentException("Script has neither data to iterate over or a generator");
        }

        if (!JdbcUtils.isValid(spec.script.execute)) {
            //throw new IllegalArgumentException("Script has no execute clause.");
            spec.script.execute = Lists.newArrayList()
        }

        spec.script.execute = pluginService.getProcessorRefs(spec.script.execute)
        spec.script.generate = pluginService.getProcessorRefs(spec.script.generate)

        val tspec = TaskSpec()
        tspec.name = spec.name
        tspec.script = Json.serializeToString(spec.script)

        val jspec = JobSpec()
        jspec.name = spec.name
        jspec.type = spec.type
        jspec.args = spec.args
        jspec.env = spec.env
        jspec.args = spec.args
        jspec.tasks = ImmutableList.of(tspec)

        return launch(jspec)
    }

    /**
     * Creating a job creates both a job record and the initial task.
     *
     * The initial task is a generator.  The generator will create more tasks
     * which map an execution pipeline to each asset generated.  The final task
     * is the reducer.
     *
     * @param spec
     */
    override fun launch(spec: JobSpec): Job {
        jobDao.nextId(spec)
        val rootPath = createSharedPaths(spec)

        /**
         * Some basic env vars.
         */
        spec.putToEnv("ZORROA_JOB_ID", spec.jobId.toString())
        spec.putToEnv("ZORROA_JOB_TYPE", spec.type.toString())
        spec.putToEnv("ZORROA_JOB_PATH_ROOT", rootPath.toString())

        for (dir in CHILD_DIRS) {
            spec.putToEnv("ZORROA_JOB_PATH_" + StringUtils.upperCase(dir),
                    rootPath.resolve(dir).toString())
        }
        /**
         * These options allow jobs to talk back to the archivist.
         */
        spec.putToEnv("ZORROA_ARCHIVIST_URL", networkEnv.privateUri.toString())

        val job = jobDao.create(spec)
        if (spec.tasks != null) {
            for (tspec in spec.tasks) {
                createTask(tspec.setJobId(job.jobId))
            }
        }
        return jobDao.get(job.id)
    }

    override fun createParentDepend(task: TaskId): Boolean {
        return taskDao.createParentDepend(task)
    }

    override fun expand(parent: Task, expand: ExpandT): List<Task> {
        if (logger.isDebugEnabled) {
            logger.debug("Expanding: {}", Json.prettyString(expand))
        }

        val job = jobDao.get(parent.jobId)

        val expandScript = Json.deserialize(expand.getScript(), Expand::class.java)
        val expandScripts = Maps.newHashMap<String, ZpsScript>()

        var parentScript: ZpsScript? = null
        if (expandScript.execute == null) {
            try {
                parentScript = Json.Mapper.readValue(File(parent.scriptPath), ZpsScript::class.java)
            } catch (e: IOException) {
                throw ArchivistWriteException("Expand with inherited execute failure", e)
            }

        }

        if (expandScript.frames != null) {

            for (exframe in expandScript.frames) {
                val key: String

                /**
                 * There are 3 places to look for the pipeline:
                 * 1. as part of the frame (the processor specified a specific pipeline)
                 * 2. the parent script itself.
                 * 3. supplied by the task itself. (usual case)
                 */

                if (exframe.processors != null) {
                    key = "frame expand: " + Json.hash(exframe.processors)
                } else if (parentScript != null) {
                    key = "parent expand:"
                } else {
                    key = "script expand:"
                }

                /**
                 * Get the script for the particular pipeline and add the data
                 * to it.  If a script doesn't exist yet, make it.
                 */
                var script: ZpsScript? = expandScripts[key]
                if (script == null) {
                    script = ZpsScript()
                    expandScripts.put(key, script)

                    when {
                        key.startsWith("parent") -> script.execute = parentScript!!.execute
                        key.startsWith("frame") -> {
                            val refs = pipelineService.validateProcessors(job.type,
                                    exframe.processors)

                            if (job.type == PipelineType.Import) {
                                refs.add(pluginService.getProcessorRef(
                                        "com.zorroa.core.collector.IndexDocumentCollector"))
                            }

                            script.execute = refs
                        }
                        key.startsWith("script") -> script.execute = expandScript.execute
                    }
                    script.name = key
                }

                script.addToOver(exframe.document)
            }
        }

        /**
         * Now group each frame into a common task.
         */
        var assetTotal = 0
        val tasks = Lists.newArrayListWithCapacity<Task>(expandScripts.size)
        for (scr in expandScripts.values) {
            val spec = TaskSpec()
            spec.jobId = parent.jobId
            spec.name = String.format("expand:%s - %d frames / %d processors",
                    scr.name, scr.overCount, scr.executeCount)
            spec.script = Json.serializeToString(scr)
            spec.order = parent.order
            spec.parentTaskId = parent.taskId
            spec.assetCount = scr.overCount
            tasks.add(createTask(spec))
            assetTotal += scr.overCount
        }

        jobDao.incrementStats(parent.jobId, TaskStatsAdder().setTotal(assetTotal))
        return tasks
    }

    override fun continueImportTask(spec: TaskSpecV): Task {
        /**
         * Use the standard pipeline if one is not set.
         */

        if (spec.pipelineId == null || spec.pipelineId <= 0) {
            val pl = pipelineDao.getStandard(PipelineType.Import)
            spec.pipelineId = pl.id
        }

        val ts = TaskSpec()
        ts.name = spec.name
        ts.jobId = spec.jobId
        ts.parentTaskId = null
        ts.order = Task.ORDER_DEFAULT
        ts.assetCount = if (spec.docs == null) 0 else spec.docs.size

        val script = ZpsScript()
        script.over = spec.docs
        script.execute = pluginService.getProcessorRefs(spec.pipelineId)
        script.execute.add(ProcessorRef()
                .setClassName("com.zorroa.core.collector.IndexDocumentCollector")
                .setLanguage("java")
                .setArgs(ImmutableMap.of<String, Any>("importId", ts.jobId)))

        val task = taskDao.create(ts)

        /**
         * Write script to disk
         */
        try {
            Json.Mapper.writeValue(File(task.scriptPath), script)
        } catch (e: IOException) {
            throw ArchivistException("Failed to add task, " + e, e)
        }

        jobDao.incrementWaitingTaskCount(task)
        taskDao.incrementDependCount(task)
        return task
    }

    override fun createTask(spec: TaskSpec): Task {
        /**
         * Create the first task which is just the script itself.
         */
        val task = taskDao.create(spec)
        try {
            Files.write(Paths.get(task.scriptPath), spec.script.toByteArray())
        } catch (e: IOException) {
            throw ArchivistException("Failed to add task, " + e, e)
        }

        jobDao.incrementWaitingTaskCount(task)
        taskDao.incrementDependCount(task)
        return task
    }

    override fun get(id: Int): Job {
        return jobDao.get(id)
    }

    override fun setJobState(job: JobId, newState: JobState, oldState: JobState): Boolean {
        return jobDao.setState(job, newState, oldState)
    }

    override fun setTaskState(task: TaskId, newState: TaskState, vararg expect: TaskState): Boolean {
        Preconditions.checkNotNull(task.taskId)
        Preconditions.checkNotNull(task.jobId)
        Preconditions.checkNotNull(newState)

        /**
         * This locks the task.
         */
        val oldState = taskDao.getState(task, true)

        if (oldState == newState) {
            return false
        }

        if (taskDao.setState(task, newState, *expect)) {
            jobDao.updateTaskStateCounts(task, newState, oldState)
            return true
        }
        return false
    }

    override fun setHost(task: TaskId, host: String) {
        taskDao.setHost(task, host)
    }

    override fun setTaskQueued(id: TaskId): Boolean {
        return setTaskState(id, TaskState.Queued, TaskState.Waiting)
    }

    override fun setTaskQueued(script: TaskId, host: String): Boolean {
        if (setTaskState(script, TaskState.Queued, TaskState.Waiting)) {
            taskDao.setHost(script, host)
            return true
        }
        return false
    }

    override fun setTaskCompleted(task: Task, exitStatus: Int): Boolean {
        logger.info("Task {} [{}] completed on host '{}' exit status: {}", task.name,
                task.id, task.host, exitStatus)
        val newState = if (exitStatus != 0) TaskState.Failure else TaskState.Success
        if (setTaskState(task, newState, TaskState.Running, TaskState.Queued)) {
            if (newState == TaskState.Success) {
                taskDao.decrementDependCount(task)
            }
            return true
        }
        return false
    }

    override fun getTasks(jobId: Int, state: TaskState): List<Task> {
        return taskDao.getAll(jobId, state)
    }

    override fun setTaskRunning(task: Task): Boolean {
        return if (setTaskState(task, TaskState.Running, TaskState.Queued)) {
            true
        } else false
    }

    override fun incrementStats(task: TaskId, adder: TaskStatsAdder) {
        taskDao.incrementStats(task.taskId, adder)
        jobDao.incrementStats(task.jobId, adder)
    }

    override fun decrementStats(task: Task) {
        taskDao.clearStats(task.taskId)
        jobDao.decrementStats(task.jobId, task.stats)
    }

    /*
    @Override
    public boolean incrementJobStats(int id, TaskStatsAdder adder) {
        return jobDao.incrementStats(id, adder);
    }

    @Override
    public boolean incrementTaskStats(int id, TaskStatsAdder adder) {
        return taskDao.incrementStats(id, adder);
    }
    */

    override fun getAll(page: Pager, filter: JobFilter): PagedList<Job> {
        return jobDao.getAll(page, filter)
    }

    override fun getAllTasks(job: Int, page: Pager): PagedList<Task> {
        return taskDao.getAll(job, page)
    }

    override fun getAllTasks(job: Int, page: Pager, filter: TaskFilter): PagedList<Task> {
        return taskDao.getAll(job, page, filter)
    }

    override fun updatePingTime(taskIds: List<Int>): Int {
        return taskDao.updatePingTime(taskIds)
    }

    /**
     * Takes jobspec with a populated job Id and create a path for the job
     * to store its data, including logs.
     *
     * @param spec
     * @return
     */
    override fun resolveJobRoot(spec: JobSpec): Path {
        val basePath = properties.getPath("zorroa.cluster.path.jobs")

        val time = DateTime()
        val formatter = DateTimeFormat.forPattern("YYYY/MM/dd")

        return basePath
                .resolve(spec.type.toString().toLowerCase())
                .resolve(formatter.print(time))
                .resolve(getUsername())
                .resolve(spec.jobId.toString())
                .toAbsolutePath()
    }

    private fun createSharedPaths(spec: JobSpec): Path {
        val rootPath = resolveJobRoot(spec)
        logger.info("creating shared paths: {}", rootPath)
        for (child in CHILD_DIRS) {
            val childPath = rootPath.resolve(child)

            val childFile = childPath.toFile()
            if (childFile.exists()) {
                logger.warn("Log file path exists: {}", childFile)
            } else {
                childFile.mkdirs()
            }
        }
        spec.rootPath = rootPath.toString()
        return rootPath
    }

    companion object {

        private val logger = LoggerFactory.getLogger(JobServiceImpl::class.java)
    }
}
