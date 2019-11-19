package com.zorroa.archivist.service

import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.BatchIndexAssetsResponse
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.repository.IndexDao
import com.zorroa.archivist.repository.KPage
import com.zorroa.archivist.schema.ProxySchema
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.task.SyncTaskExecutor
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.nio.file.Paths
import javax.annotation.PostConstruct

/**
 * The IndexService is responsible for the business logic around asset CRUD and batch operations.
 */
interface IndexService {

    fun getMapping(): Map<String, Any>

    fun get(id: String): Document

    fun get(path: Path): Document

    fun getAll(ids: List<String>): List<Document>

    fun getAll(page: KPage): List<Document>

    fun getProxies(id: String): ProxySchema

    fun index(assets: List<Document>): BatchIndexAssetsResponse

    fun index(doc: Document): Document

    fun exists(id: String): Boolean

    fun delete(assetId: String): Boolean
}

@Component
class IndexServiceImpl @Autowired constructor(
    private val indexDao: IndexDao,
    private val fileServerProvider: FileServerProvider,
    private val fileStorageService: FileStorageService

) : IndexService {

    lateinit var workQueue: TaskExecutor

    @PostConstruct
    fun init() {
        workQueue = buildAssetWorkQueue()
    }

    override fun get(id: String): Document {
        return if (id.startsWith("/")) {
            get(Paths.get(id))
        } else {
            indexDao.get(id)
        }
    }

    override fun get(path: Path): Document {
        return indexDao.get(path)
    }

    override fun getAll(ids: List<String>): List<Document> {
        return indexDao.getAll(ids)
    }

    override fun getAll(page: KPage): List<Document> {
        return indexDao.getAll(page)
    }

    override fun getProxies(id: String): ProxySchema {
        val asset = get(id)
        val proxies = asset.getAttr("proxies", ProxySchema::class.java)
        return proxies ?: ProxySchema()
    }

    override fun index(doc: Document): Document {
        index(listOf(doc))
        return indexDao.get(doc.id)
    }

    override fun index(assets: List<Document>): BatchIndexAssetsResponse {
        // TODO: Write to GCS or local storage
        // Register file to asset
        return indexDao.index(assets, true)
    }

    override fun exists(id: String): Boolean {
        return indexDao.exists(id)
    }

    override fun delete(assetId: String): Boolean {
        val doc = indexDao.get(assetId)
        val result = indexDao.delete(doc)
        deleteAssociatedFiles(doc)
        return result
    }

    fun deleteAssociatedFiles(doc: Document) {

        doc.getAttr("proxies", ProxySchema::class.java)?.let {
            it.proxies?.forEach { pr ->
                try {
                    val storage = fileStorageService.get(pr.id)
                    val ofile = fileServerProvider.getServableFile(storage.uri)
                    if (ofile.delete()) {
                        logger.event(
                            LogObject.STORAGE, LogAction.DELETE,
                            mapOf("proxyId" to pr.id, "assetId" to doc.id)
                        )
                    } else {
                        logger.warnEvent(
                            LogObject.STORAGE, LogAction.DELETE, "file did not exist",
                            mapOf("proxyId" to pr.id)
                        )
                    }
                } catch (e: Exception) {
                    logger.warnEvent(
                        LogObject.STORAGE, LogAction.DELETE, e.message ?: e.javaClass.name,
                        mapOf("proxyId" to pr.id), e
                    )
                }
            }
        }
    }

    private fun buildAssetWorkQueue(): TaskExecutor {
        return if (ArchivistConfiguration.unittest) {
            SyncTaskExecutor()
        } else {
            val tpe = ThreadPoolTaskExecutor()
            tpe.corePoolSize = 4
            tpe.maxPoolSize = 4
            tpe.threadNamePrefix = "ASSET-QUEUE-"
            tpe.isDaemon = true
            tpe.setQueueCapacity(1000)
            tpe.initialize()
            return tpe
        }
    }

    override fun getMapping(): Map<String, Any> {
        return indexDao.getMapping()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(IndexServiceImpl::class.java)
    }
}
