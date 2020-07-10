package com.zorroa.archivist.service

import com.google.cloud.automl.v1.AutoMlClient
import com.google.common.util.concurrent.AbstractScheduledService
import com.google.longrunning.OperationsClient
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.AutomlSession
import com.zorroa.archivist.domain.AutomlSessionSpec
import com.zorroa.archivist.domain.AutomlSessionState
import com.zorroa.archivist.domain.Model
import com.zorroa.archivist.domain.Provider
import com.zorroa.archivist.repository.AutomlDao
import com.zorroa.archivist.repository.UUIDGen
import com.zorroa.archivist.security.InternalThreadAuthentication
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.security.getZmlpActor
import com.zorroa.archivist.security.withAuth
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.zmlp.service.logging.event
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.Closeable
import java.util.concurrent.TimeUnit

interface AutomlService {
    fun createSession(model: Model, spec: AutomlSessionSpec): AutomlSession
    fun checkAutomlTrainingStatus()
    fun automlClient(): AutomlClientWrapper
}

@Service
@Transactional
class AutomlServiceImpl(
    val automlDao: AutomlDao
) : AutomlService {

    @Autowired
    lateinit var modelService: ModelService

    override fun createSession(model: Model, spec: AutomlSessionSpec): AutomlSession {

        if (model.type.provider != Provider.GOOGLE) {
            throw IllegalArgumentException("Cannot use this model type with AutoML")
        }

        val time = System.currentTimeMillis()
        val id = UUIDGen.uuid1.generate()
        val actor = getZmlpActor()

        val session = AutomlSession(
            id,
            model.id,
            getProjectId(),
            spec.automlDataSet,
            spec.automlTrainingJob,
            null,
            null,
            AutomlSessionState.TRAINING,
            time,
            time,
            actor.toString(),
            actor.toString()
        )

        logger.event(
            LogObject.AUTOML, LogAction.CREATE,
            mapOf(
                "automlId" to id,
                "automlDataSet" to spec.automlDataSet,
                "automlTrainingJob" to spec.automlTrainingJob
            )
        )

        return automlDao.save(session)
    }

    override fun checkAutomlTrainingStatus() {
        automlClient().use { client ->
            for (session in automlDao.findByState(AutomlSessionState.TRAINING)) {
                logger.info("Found training session: ${session.id}")
                try {
                    val op = client.getOperationsClient().getOperation(session.automlTrainingJob)
                    println(op)
                    if (op.hasError()) {
                        val msg = "[${op.error.code}] ${op.error.message}"
                        logger.warn("Auto ML job failed, $msg")
                        automlDao.setError(msg, session.id)
                    } else if (op.done) {
                        val rsp = op.response
                        val gmod = rsp.unpack(com.google.cloud.automl.v1.Model::class.java)
                        automlDao.setFinished(gmod.name, session.id)

                        // Publish the model.
                        withAuth(InternalThreadAuthentication(session.projectId, setOf())) {
                            val model = modelService.getModel(session.modelId)
                            modelService.publishModel(model,
                                mapOf("automl_model_id" to gmod.name))
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to obtain AutoML training job", e)
                }
            }
        }
    }

    override fun automlClient(): AutomlClientWrapper {
        return AutomlClientWrapper()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AutomlServiceImpl::class.java)
    }
}

/**
 * Wraps an AutoMlClient client instance so its easier to mock.
 */
class AutomlClientWrapper : Closeable {

    val client: AutoMlClient

    init {
        client = AutoMlClient.create()
    }

    override fun close() {
        client.close()
    }

    fun getOperationsClient(): OperationsClient {
        return client.operationsClient
    }
}

/**
 * Handles checking for AutoML job status.
 */
class AutomlStateManager(
    val automlService: AutomlService
) : AbstractScheduledService() {

    init {
        if (!ArchivistConfiguration.unittest) {
            startAsync()
        }
    }

    override fun runOneIteration() {
        try {
            automlService.checkAutomlTrainingStatus()
        } catch (e: Exception) {
            logger.warn("Failed to check on AutoML job states", e)
        }
    }

    override fun scheduler(): Scheduler {
        return Scheduler.newFixedDelaySchedule(120, 306, TimeUnit.SECONDS)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AutomlStateManager::class.java)
    }
}
