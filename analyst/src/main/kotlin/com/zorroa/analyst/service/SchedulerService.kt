package com.zorroa.analyst.service

import com.zorroa.common.domain.*
import com.zorroa.common.util.Json
import com.zorroa.common.util.getPublicUrl
import io.fabric8.kubernetes.api.model.Container
import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.Quantity
import io.fabric8.kubernetes.api.model.VolumeMount
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder
import io.fabric8.kubernetes.api.model.extensions.DeploymentSpec
import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.internal.SerializationUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PostConstruct
import kotlin.concurrent.fixedRateTimer
import io.fabric8.kubernetes.api.model.Job as KJob

interface SchedulerService {
    fun startJob(job: Job) : Boolean
    fun pause(): Boolean
    fun resume() : Boolean
    fun schedule()
}


@Configuration
@ConfigurationProperties("analyst.scheduler")
class SchedulerProperties {
    var type: String? = "local"
    var k8 : Map<String, Any>? = null
}

class K8SchedulerProperties {
    var url: String? = null
    var username: String? = null
    var password: String? = null
    var namespace: String = "default"
    var image: String? = null
}

/**
 * This will eventually replicate the old Analyst behavior.
 */
class LocalSchedulerServiceImpl: SchedulerService {
    override fun schedule() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun startJob(job: Job): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun pause(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun resume(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

/**
 * A service for launching jobs for data processing to Kubernetes
 */

class K8SchedulerServiceImpl constructor(val k8Props : K8SchedulerProperties) : SchedulerService {

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var storageService: StorageService

    @Autowired
    lateinit var pipelineService: PipelineService

    @Value("\${cdv.url}")
    lateinit var cdvUrl : String

    private val kubernetesClient : KubernetesClient

    /**
     * A fixedRateTimer
     */
    private var timer : Timer? = null

    /**
     * Determines if the scheduler is paused or not
     */
    private val paused = AtomicBoolean(false)

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

    @PostConstruct
    fun start() {
        timer = fixedRateTimer("scheduler",
                daemon = true, initialDelay = 60000, period = 60000) {
            if (!paused.get()) {
                try {
                    schedule()
                } catch (e: Exception) {
                    logger.warn("Scheduler failed to run: {}", e)
                }
            }
        }
    }

    override fun startJob(job: Job) : Boolean {
        val started= jobService.start(job)
        if (!started) {
            return false
        }
        try {
            val selfUrl = getPublicUrl()
            val yaml = generateK8JobSpec(job, selfUrl)
            logger.info("JOB: {}", job.id)
            logger.info("YAML {}", yaml)
            kubernetesClient.extensions().jobs().load(
                    yaml.byteInputStream(Charsets.UTF_8)).create()
        } catch (e: Exception) {
            jobService.setState(job, JobState.RUNNING, JobState.FAIL)
            return false
        }

        return true
    }

    fun generateZpsScript(job: Job, selfUrl: String) : ZpsScript {

        /*
         * Resolve the list of processors and append one to talk back
         */
        logger.info("Building job with pipelines: {}", job.pipelines)
        val endpoint = "$selfUrl/api/v1/assets/${job.assetId}?job=${job.id}"
        val processors =
                pipelineService.buildProcessorList(job.pipelines)
        processors.add(
                ProcessorRef("zplugins.metadata.metadata.PostMetadataToRestApi",
                args=mapOf("endpoint" to endpoint)))

        val doc = try {
            //assetService.getDocument(asset)
            Document(job.assetId.toString())
        }
        catch (e : Exception) {
            Document(job.assetId.toString())
        }

        // make sure these are set.
        doc.setAttr("irm.companyId", job.attrs["companyId"])
        doc.setAttr("zorroa.organizationId", job.organizationId.toString())
        /*
         * Setup ZPS script
         */
        return ZpsScript(job.name,
                over=listOf(doc),
                execute=processors)
    }

    fun generateK8JobSpec(job: Job, selfUrl: String) : String {

        val zps = generateZpsScript(job, selfUrl)
        logger.info("ZPS: {}", Json.serializeToString(zps))

        val url = storageService.storeSignedBlob(
                "zorroa/jobs/${job.id}.zps",
                "application/json",
                Json.serialize(zps))

        val labels = mapOf(
                "assetId" to job.assetId.toString(),
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
        mount.name = "google-cloud-key"
        mount.mountPath = "/var/secrets/google"

        val container = Container()
        container.resources.limits = mapOf("cpu" to Quantity("1"))
        container.resources.requests = mapOf("cpu" to Quantity("1"))
        container.args = listOf("-cpus", "1")
        container.volumeMounts = listOf(mount)
        container.name = job.name
        container.image = k8Props.image
        container.args = listOf(url.toString())
        container.env = mutableListOf(
                EnvVar("ZORROA_ANALYST_BASE_URL", selfUrl, null),
                EnvVar("OFS_CLASS", "cdv", null),
                EnvVar("ZORROA_ORGANIZATION_ID", job.organizationId.toString(), null),
                EnvVar("CDV_COMPANY_ID", job.attrs["companyId"].toString(), null),
                EnvVar("CDV_DOCUMENT_GUID", job.assetId.toString(),null),
                EnvVar("CDV_API_BASE_URL", cdvUrl, null),
                EnvVar("GOOGLE_APPLICATION_CREDENTIALS", " /var/secrets/google/key.json", null))

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
                    .withName("google-cloud-key")
                    .withNewSecret()
                        .withSecretName("zorroa-workers-key")
                    .endSecret()
                .endVolume()
                .withRestartPolicy("Never")
                .addAllToContainers(listOf(container)).endSpec()
        template.endTemplate().endSpec()
        return SerializationUtils.dumpAsYaml(builder.build())
    }

    override fun pause() : Boolean = paused.compareAndSet(false, true)
    override fun resume() : Boolean = paused.compareAndSet(true, false)

    override fun schedule() {
        val runningJobs = jobService.getRunning()
        logger.info("Analyst has {} running jobs", runningJobs)
        validateRunning(runningJobs)

        if (runningJobs.size < 10) {
            val jobs = jobService.getWaiting(10 - runningJobs.size)
            logger.info("Found {} waiting jobs", jobs.size)

            for (job in jobs) {
                startJob(job)
            }
        }
    }

    private fun validateRunning(jobs:List<Job>) {
        var orphanJobs = 0
        for (job in jobs) {
            val kjob: io.fabric8.kubernetes.api.model.Job? = kubernetesClient.extensions().jobs().withName(job.name).get()
            if (kjob != null) {
                // Remove this later
                logger.info("{}", Json.prettyString(kjob.status))
                if (kjob.status.succeeded >= 1) {
                    jobService.setState(job, JobState.SUCCESS, JobState.RUNNING)
                    kubernetesClient.extensions().jobs().delete(kjob)
                }
                else {
                    for (cond in kjob.status.conditions) {
                        if (cond.type == "Failed") {
                            jobService.setState(job, JobState.FAIL, JobState.RUNNING)
                            break
                        }
                    }
                }
            }
            else {
                orphanJobs++
                // Have to decide what to do here
            }
        }
        if (orphanJobs > 0) {
            logger.warn("Analyst had {} orphan jobs", orphanJobs)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(K8SchedulerServiceImpl::class.java)
    }
}
