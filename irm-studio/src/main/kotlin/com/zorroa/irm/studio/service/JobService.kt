package com.zorroa.irm.studio.service

import com.zorroa.common.domain.*
import com.zorroa.common.util.Json
import com.zorroa.irm.studio.repository.JobDao
import io.fabric8.kubernetes.api.model.Container
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder
import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.internal.SerializationUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import java.util.*
import io.fabric8.kubernetes.api.model.Job as KJob


interface JobService {
    fun start(job: com.zorroa.common.domain.Job) : Any
    fun finish(id: UUID, doc: Document)
    fun create(spec: JobSpec) : com.zorroa.common.domain.Job
    fun get(id: UUID) : com.zorroa.common.domain.Job
}

@Configuration
@ConfigurationProperties("gcp.k8")
class K8JobServiceSettings {

    var url: String? = null
    var username: String? = null
    var password: String? = null
    var namespace: String = "default"
    var image: String? = null
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

    override fun start(job: Job) : Job {
        val yaml = generateK8JobSpec(job)
        val kjob = kubernetesClient.extensions().jobs().load(
                yaml.byteInputStream(Charsets.UTF_8)).create()
        return job
    }

    override fun finish(id: UUID, doc: Document) {
        try {

            val job = jobDao.get(id)
            if (jobDao.setState(job, JobState.SUCCESS, JobState.RUNNING)) {
                assetService.storeAndReindex(job.organizationId, doc)
            }
            else {
                logger.warn("Failed to set job state: {} to {}", job.id, JobState.RUNNING)
            }

        } finally {
            logger.warn("Failed to set job to success [$id]", doc)
        }
    }

    fun generateZpsScript(job: Job) : ZpsScript {
        /*
         * Resolve the list of processors and append one to talk back
         */
        logger.info("Building job with pipelines: {}", job.pipelines)

        val endpoint = "http://35.231.205.98/api/v1/jobs/${job.id}/result"

        val processors = pipelineService.buildProcessorList(job.pipelines)
        processors.add(ProcessorRef("zplugins.metadata.metadata.PostMetadataToRestApi",
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

        val builder = DeploymentBuilder()
                .withApiVersion("batch/v1")
                .withKind("Job")
        builder.editOrNewMetadata().withName(job.name).endMetadata()

        val container = Container()
        container.name = job.name
        container.image = k8settings.image
        container.args = listOf(url.toString())

        val spec = builder.withNewSpec()
        val template = spec.editOrNewTemplate()
        template.editOrNewMetadata().withName(job.name).endMetadata()
        template.editOrNewSpec()
                .withRestartPolicy("Never")
                .addAllToContainers(listOf(container)).endSpec()
        template.endTemplate().endSpec()
        return SerializationUtils.dumpAsYaml(builder.build())
    }

    override fun get(id: UUID) : Job {
        return jobDao.get(id)
    }

    override fun create(spec: JobSpec) : Job {
        return jobDao.create(spec)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(K8JobServiceImpl::class.java)

    }


}
