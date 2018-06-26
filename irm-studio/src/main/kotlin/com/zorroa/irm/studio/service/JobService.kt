package com.zorroa.irm.studio.service

import com.zorroa.common.domain.*
import com.zorroa.common.util.Json
import com.zorroa.irm.studio.repository.JobDao
import io.fabric8.kubernetes.api.model.Container
import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.EnvVarSource
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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import java.util.*
import io.fabric8.kubernetes.api.model.Job as KJob


interface JobService {
    fun start(job: Job) : Boolean
    fun finish(job: Job, doc: Document) : Boolean
    fun create(spec: JobSpec) : Job
    fun get(id: UUID) : Job
    fun get(name: String) : Job
}

@Configuration
@ConfigurationProperties("gcp.k8")
class K8JobServiceSettings {

    var url: String? = null
    var username: String? = null
    var password: String? = null
    var namespace: String = "default"
    var image: String? = null
    var talkBackUrl : String? = null
}

@Configuration
class K8ClientConfiguration {

    @Autowired
    lateinit var k8settings : K8JobServiceSettings

    @Bean
    fun kubernetesClient() : KubernetesClient {
        val k8Config = ConfigBuilder().apply {
            k8settings.url?.let { withMasterUrl(k8settings.url) }
            k8settings.username?.let { withUsername(k8settings.username) }
            k8settings.password?.let { withPassword(k8settings.password) }
            withNamespace(k8settings.namespace)
            withCaCertFile("/keys/dit1.cert")
        }.build()
        return DefaultKubernetesClient(k8Config)
    }
}

/**
 * A service for launching jobs for data processing to Kubernetes
 */
@Service
class K8JobServiceImpl @Autowired constructor(
        val jobDao: JobDao
): JobService {

    @Autowired
    lateinit var storageService: StorageService

    @Autowired
    lateinit var pipelineService: PipelineService

    @Autowired
    lateinit var assetService: AssetService

    @Autowired
    lateinit var kubernetesClient : KubernetesClient

    @Autowired
    lateinit var k8settings : K8JobServiceSettings

    @Value("\${irm.cdv.url}")
    lateinit var cdvUrl : String

    override fun start(job: Job) : Boolean {
        val started= jobDao.setState(job, JobState.RUNNING, JobState.WAITING)
        logger.info("Job {} WAITING->RUNNING: {}", job.name, started)
        if (!started) {
            return false
        }
        val yaml = generateK8JobSpec(job)
        val kjob = kubernetesClient.extensions().jobs().load(
                yaml.byteInputStream(Charsets.UTF_8)).create()
        return true
    }

    override fun finish(job: Job, doc: Document) : Boolean {
        val result = jobDao.setState(job, JobState.SUCCESS, JobState.RUNNING)
        logger.info("Job {} RUNNING->SUCCESS: {}", job.name, result)
        return result
    }

    fun generateZpsScript(job: Job) : ZpsScript {
        /*
         * Resolve the list of processors and append one to talk back
         */
        logger.info("Building job with pipelines: {}", job.pipelines)
        val endpoint = "${k8settings.talkBackUrl}/api/v1/jobs/${job.id}/result"
        val processors =
                pipelineService.buildProcessorList(job.pipelines)
        processors.add(
                ProcessorRef("zplugins.metadata.metadata.PostMetadataToRestApi",
                args=mapOf("serializer" to "core_data_vault", "endpoint" to endpoint)))
        /*
        * Pull the current state of the asset
        */
        val asset = try {
            assetService.getDocument(job.organizationId, job.assetId)
        }
        catch (e : Exception) {
            Document(job.assetId.toString(), mapOf())
        }
        /*
         * Setup ZPS script
         */
        return ZpsScript(job.name,
                over=listOf(asset),
                execute=processors)
    }

    fun generateK8JobSpec(job: Job) : String {

        val zps = generateZpsScript(job)
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

        val container = Container()
        container.name = job.name
        container.image = k8settings.image
        container.args = listOf(url.toString())
        container.env = mutableListOf(
                EnvVar("OFS_CLASS", "cdv", null),
                EnvVar("ZORROA_ORGANIZATION_ID", job.organizationId.toString(), null),
                EnvVar("CDV_COMPANY_ID", job.attrs["companyId"].toString(), null),
                EnvVar("CDV_DOCUMENT_GUID", job.assetId.toString(),null),
                EnvVar("CDV_API_BASE_URL", cdvUrl, null),
                EnvVar("ZORROA_STUIDO_BASE_URL", k8settings.talkBackUrl, null))

        val dspec = DeploymentSpec()
        dspec.setAdditionalProperty("backOffLimit", 1)
        val spec = builder.withNewSpecLike(dspec)
        val template = spec.editOrNewTemplate()
        template.editOrNewMetadata()
                .withName(job.name)
                .withLabels(labels)
                .endMetadata()
        template.editOrNewSpec()
                .withRestartPolicy("Never")
                .addAllToContainers(listOf(container)).endSpec()
        template.endTemplate().endSpec()
        return SerializationUtils.dumpAsYaml(builder.build())
    }

    override fun get(id: UUID) : Job {
        return jobDao.get(id)
    }

    override fun get(name: String) : Job {
        return jobDao.get(name)
    }

    override fun create(spec: JobSpec) : Job {
        return jobDao.create(spec)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(K8JobServiceImpl::class.java)

    }
}
