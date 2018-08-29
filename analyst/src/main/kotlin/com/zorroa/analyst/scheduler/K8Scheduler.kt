package com.zorroa.analyst.scheduler

import com.zorroa.analyst.service.JobService
import com.zorroa.analyst.service.JobStorageService
import com.zorroa.analyst.service.SchedulerProperties
import com.zorroa.analyst.service.SchedulerService
import com.zorroa.common.domain.Job
import com.zorroa.common.domain.JobState
import com.zorroa.common.server.NetworkEnvironment
import com.zorroa.common.util.Json
import io.fabric8.kubernetes.api.model.Container
import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.VolumeMount
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder
import io.fabric8.kubernetes.api.model.extensions.DeploymentSpec
import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.internal.SerializationUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant


class K8SchedulerProperties {
    var url: String? = null
    var username: String? = null
    var password: String? = null
    var namespace: String = "default"
    var image: String? = null
}

/**
 * A service for launching jobs for data processing to Kubernetes
 */

class K8SchedulerServiceImpl constructor(val k8Props : K8SchedulerProperties) : SchedulerService {

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var storageService: JobStorageService

    @Autowired
    lateinit var schedulerProperties : SchedulerProperties

    @Autowired
    lateinit var networkEnvironment: NetworkEnvironment

    private val kubernetesClient : KubernetesClient

    init {
        val k8Config = ConfigBuilder().apply {
            k8Props.url?.let { withMasterUrl(k8Props.url) }
            k8Props.username?.let { withUsername(k8Props.username) }
            k8Props.password?.let { withPassword(k8Props.password) }
            withNamespace(k8Props.namespace)
            withCaCertFile("/config/k8.cert")
        }.build()
        kubernetesClient = DefaultKubernetesClient(k8Config)
    }

    override fun runJob(job: Job) : Boolean {
        // will throw on failure to stat
        jobService.start(job)

        try {
            val yaml = generateK8JobSpec(job)
            logger.info("JOB: {}", job.id)
            logger.info("YAML {}", yaml)

            val job = kubernetesClient.extensions().jobs().load(
                    yaml.byteInputStream(Charsets.UTF_8)).create()

        } catch (e: Exception) {
            logger.warn("Failed to start job {}", job.name, e)
            jobService.stop(job, JobState.Fail)
            return false
        }

        return true
    }

    override fun kill(job: Job) : Boolean {
        val jobs =
                kubernetesClient.extensions().jobs().withLabel("jobId", job.id.toString()).list()
        if (!jobs.items.isEmpty()) {
            for (job in jobs.items) {
                logger.info("deleting : {}", job.metadata.name)
                kubernetesClient.extensions().jobs().delete(job)
            }
        }

        val killed = if (job.state == JobState.Running) {
            jobService.stop(job, JobState.Fail)
        }
        else {
            jobService.setState(job, JobState.Fail, null)
        }

        if (killed) {
            jobService.clearLocks(job)
        }
        return killed
    }

    override fun retry(job: Job) : Boolean {

        val jobs =
                kubernetesClient.extensions().jobs().withLabel("jobId", job.id.toString()).list()
        if (!jobs.items.isEmpty()) {
            for (job in jobs.items) {
                logger.info("deleting : {}", job.metadata.name)
                kubernetesClient.extensions().jobs().delete(job)
            }
        }

        if (job.state == JobState.Running) {
            jobService.stop(job, JobState.Waiting)
        }
        else {
            jobService.setState(job, JobState.Waiting, null)
        }

        jobService.clearLocks(job)
        return runJob(job)
    }


    fun generateK8JobSpec(job: Job) : String {
        val labels = mapOf(
                "companyId" to job.attrs["companyId"].toString(),
                "organizationId" to job.organizationId.toString(),
                "jobId" to job.id.toString(),
                "type" to "ingest",
                "script" to  "zps")

        val builder = DeploymentBuilder()
                .withApiVersion("batch/v1")
                .withKind("Job")
                .withNewMetadata()
                .withName(job.name)
                .withLabels(labels)
                .endMetadata()

        val mount = VolumeMount()
        mount.name = "secrets-volume"
        mount.mountPath = "/var/secrets/google"

        val container = Container()
        container.args = listOf("-cpus", "1")
        container.volumeMounts = listOf(mount)
        container.name = job.name
        container.image = k8Props.image
        container.args = listOf(storageService.getSignedUrl(
                networkEnvironment.getBucket("zorroa-job-data"), job.getScriptPath()).toString())
        container.env = mutableListOf(
                EnvVar("ZORROA_JOB_ID", job.id.toString(), null),
                EnvVar("ZORROA_ORGANIZATION_ID", job.organizationId.toString(), null),
                EnvVar("OFS_CLASS", "cdv", null),
                EnvVar("CDV_COMPANY_ID", job.attrs["companyId"].toString(), null),
                EnvVar("CDV_API_BASE_URL", networkEnvironment.getPublicUrl("core-data-vault-api"), null),
                EnvVar("GOOGLE_APPLICATION_CREDENTIALS", "/var/secrets/google/credentials.json", null),
                EnvVar("CDV_GOOGLE_CREDENTIAL_PATH", "/var/secrets/google/cdv.json", null),
                EnvVar("ZORROA_ARCHIVIST_URL", networkEnvironment.getPublicUrl("zorroa-archivist"), null))

        for ((k,v) in job.env) {
            container.env.add(EnvVar(k, v, null))
        }

        val dspec = DeploymentSpec()
        dspec.setAdditionalProperty("backOffLimit", 1)
        val spec = builder.withNewSpecLike(dspec)
        val template = spec.editOrNewTemplate()
        template.editOrNewMetadata()
                .withName(job.name)
                .withLabels(labels)
                .endMetadata()
        template.editOrNewSpec()
                .addNewVolume()
                .withName("secrets-volume")
                .withNewSecret()
                .withSecretName("analyst-secrets")
                .endSecret()
                .endVolume()
                .withRestartPolicy("Never")
                .addAllToContainers(listOf(container)).endSpec()
        template.endTemplate().endSpec()
        return SerializationUtils.dumpAsYaml(builder.build())
    }

    override fun schedule() {
        val runningJobs = jobService.getRunning()
        logger.info("Analyst has {} running jobs", runningJobs)
        try {
            validateRunning(runningJobs)
        } catch (e: Exception) {
            logger.warn("failed to validate running jobs", e)
        }

        try {
            removeOldJobs()
        }
        catch(e: Exception) {
            logger.warn("Error removing old K8 jobs,", e)
        }

        if (runningJobs.size < schedulerProperties.maxJobs) {
            val jobs = jobService.queueWaiting(
                    schedulerProperties.maxJobs - runningJobs.size)
            logger.info("Found {} waiting jobs", jobs.size)

            for (job in jobs) {
                runJob(job)
            }
        }
    }

    private fun validateRunning(jobs:List<Job>) {
        var orphanJobs = 0
        for (job in jobs) {
            val kjob: io.fabric8.kubernetes.api.model.Job? =
                    kubernetesClient.extensions().jobs().withName(job.name).get()
            if (kjob != null) {
                if (kjob.status != null) {
                    if (kjob.status.succeeded != null) {
                        if (kjob.status.succeeded >= 1) {
                            jobService.stop(job, JobState.Success)
                        }
                    }
                    else if (kjob.status.conditions != null) {
                        for (cond in kjob.status.conditions) {
                            if (cond.type == "Failed") {
                                jobService.stop(job, JobState.Fail)
                                break
                            }
                        }
                    }
                }
                else {
                    logger.warn("Job has no status data: {}", job.name)
                    logger.warn(Json.prettyString(kjob))
                }
            }
            else {
                orphanJobs++
                // Orphans are jobs running in the DB with no K8 job.
                jobService.stop(job, JobState.Orphan)
            }
        }
        if (orphanJobs > 0) {
            logger.warn("Analyst had {} orphan jobs", orphanJobs)
        }
    }

    private fun removeOldJobs() {
        val now = Instant.now().epochSecond
        val jobs = kubernetesClient.extensions().jobs().list()
        for (job in jobs.items) {
            if (job.status != null) {
                /**
                 * First delete by completion time
                 */
                if (job.status.completionTime != null) {
                    val i =  Instant.parse(job.status.completionTime)
                    if (checkExpired(now, i)) {
                        logger.info("Removing job: {}", job.metadata.name)
                        kubernetesClient.extensions().jobs().delete(job)
                    }
                }
                /**
                 * Then check failures which have no completion time.
                 */
                if (job.status.conditions != null) {
                    for (cond in job.status.conditions) {
                        if (cond.type != null) {
                            if (cond.type == "Failed") {
                                val i =  Instant.parse(cond.lastTransitionTime)
                                if (checkExpired(now, i)) {
                                    logger.info("Removing job: {}", job.metadata.name)
                                    kubernetesClient.extensions().jobs().delete(job)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkExpired(now: Long, i: Instant) : Boolean {
        return (now - i.epochSecond) / 3600.0 > schedulerProperties.maxJobAgeHours
    }

    companion object {
        private val logger = LoggerFactory.getLogger(K8SchedulerServiceImpl::class.java)
    }
}
