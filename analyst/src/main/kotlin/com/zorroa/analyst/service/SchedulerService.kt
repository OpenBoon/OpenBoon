package com.zorroa.analyst.service

import com.zorroa.common.domain.*
import com.zorroa.common.util.Json
import com.zorroa.analyst.repository.JobDao
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
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import java.util.*
import io.fabric8.kubernetes.api.model.Job as KJob


interface SchedulerService {
    fun start(job: Job) : Boolean
}

@Configuration
@ConfigurationProperties("gcp.k8")
class K8SchedulerServiceSettings {

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
    lateinit var k8settings : K8SchedulerServiceSettings

    @Bean
    fun kubernetesClient() : KubernetesClient {
        val k8Config = ConfigBuilder().apply {
            k8settings.url?.let { withMasterUrl(k8settings.url) }
            k8settings.username?.let { withUsername(k8settings.username) }
            k8settings.password?.let { withPassword(k8settings.password) }
            withNamespace(k8settings.namespace)
            withCaCertFile("/config/k8.cert")
        }.build()
        return DefaultKubernetesClient(k8Config)
    }
}

/**
 * A service for launching jobs for data processing to Kubernetes
 */
@Service
class K8SchedulerServiceImpl @Autowired constructor(
        val jobDao: JobDao
): SchedulerService {

    @Autowired
    lateinit var storageService: StorageService

    @Autowired
    lateinit var pipelineService: PipelineService

    @Autowired
    lateinit var kubernetesClient : KubernetesClient

    @Autowired
    lateinit var k8settings : K8SchedulerServiceSettings

    @Value("\${cdv.url}")
    lateinit var cdvUrl : String

    override fun start(job: Job) : Boolean {
        val started= jobDao.setState(job, JobState.RUNNING, JobState.WAITING)
        logger.info("Job {} WAITING->RUNNING: {}", job.name, started)
        if (!started) {
            return false
        }
        val yaml = generateK8JobSpec(job)
        logger.info("JOB: {}", job.id)
        logger.info("YAML {}", yaml)
        val kjob = kubernetesClient.extensions().jobs().load(
                yaml.byteInputStream(Charsets.UTF_8)).create()
        return true
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
                args=mapOf("endpoint" to endpoint)))
        /*
        * Pull the current state of the asset
        */
        val asset = Asset(job.assetId, job.organizationId, job.attrs)
        val doc = try {
            //assetService.getDocument(asset)
            Document(job.assetId.toString())
        }
        catch (e : Exception) {
            Document(job.assetId.toString())
        }

        // make sure this is et.
        doc.setAttr("irm.companyId", job.attrs["companyId"])

        /*
         * Setup ZPS script
         */
        return ZpsScript(job.name,
                over=listOf(doc),
                execute=processors)
    }

    fun generateK8JobSpec(job: Job) : String {

        val zps = generateZpsScript(job)
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
        container.volumeMounts = listOf(mount)
        container.name = job.name
        container.image = k8settings.image
        container.args = listOf(url.toString())
        container.env = mutableListOf(
                EnvVar("ZORROA_STUDIO_BASE_URL", k8settings.talkBackUrl, null),
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

    companion object {
        private val logger = LoggerFactory.getLogger(K8SchedulerServiceImpl::class.java)
    }
}
