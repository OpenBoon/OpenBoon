package com.zorroa.archivist

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.zorroa.archivist.clients.ApiKey
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.Source
import com.zorroa.archivist.security.AnalystAuthentication
import com.zorroa.archivist.service.*
import com.zorroa.archivist.util.FileUtils
import com.zorroa.common.schema.Proxy
import com.zorroa.common.schema.ProxySchema
import com.zorroa.common.util.Json
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.client.RequestOptions
import org.junit.Before
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionCallbackWithoutResult
import org.springframework.transaction.support.TransactionTemplate
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import javax.sql.DataSource

@RunWith(SpringRunner::class)
@SpringBootTest
@TestPropertySource(locations = ["classpath:test.properties"])
@WebAppConfiguration
@Transactional
abstract class AbstractTest {

    val logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    protected lateinit var fileServerProvider: FileServerProvider

    @Autowired
    protected lateinit var indexService: IndexService

    @Autowired
    protected lateinit var assetService: AssetService

    @Autowired
    protected lateinit var properties: ApplicationProperties

    @Autowired
    internal lateinit var indexRoutingService: IndexRoutingService

    @Autowired
    internal lateinit var transactionEventManager: TransactionEventManager

    @Autowired
    internal lateinit var transactionManager: DataSourceTransactionManager

    protected lateinit var jdbc: JdbcTemplate

    protected lateinit var resources: Path

    init {
        ArchivistConfiguration.unittest = true
    }

    fun requiresElasticSearch(): Boolean {
        return false
    }

    fun requiresFieldSets(): Boolean {
        return false
    }

    @Autowired
    fun setDataSource(dataSource: DataSource) {
        this.jdbc = JdbcTemplate(dataSource)
    }

    @Before
    @Throws(IOException::class)
    fun setup() {
        /*
         * Ensures that all transaction events run within the unit test transaction.
         * If this was not set then  transaction events like AfterCommit would never execute
         * because unit test transactions are never committed.
         */
        transactionEventManager.isImmediateMode = true

        /**
         * Before we can do anything reliably we need a logged in user.
         */
        authenticate()

        /**
         * We need to be authed to clean elastic.
         */
        if (requiresElasticSearch()) {
            cleanElastic()
        }

        Json.Mapper.registerModule(KotlinModule())
    }

    fun authenticateAsAnalyst() {
        SecurityContextHolder.getContext().authentication = AnalystAuthentication("https://127.0.0.1:5000")
    }
    
    fun deleteAllIndexes() {
        /*
         * The Elastic index(s) has been created, but we have to delete it and recreate it
         * so each test has a clean index.  Once this is done we can call setupDataSources()
         * which adds some standard data to both databases.
         */
        val rest = indexRoutingService.getOrgRestClient()
        val reqDel = DeleteIndexRequest("_all")

        /*
         * Delete will throw here if the index doesn't exist.
         */
        try {
            rest.client.indices().delete(reqDel, RequestOptions.DEFAULT)
        } catch (e: Exception) {
            logger.warn("Failed to delete 'unittest' index, this is usually ok.")
        }
    }

    fun cleanElastic() {
        deleteAllIndexes()

        // See setup() method for configuration of default index.
        indexRoutingService.syncAllIndexRoutes()
    }

    /**
     * Authenticates a user as admin but with all permissions, including internal ones.
     */
    fun authenticate() {
        val apiKey = ApiKey(
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                listOf("SEARCH"))
        
        SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(
                        apiKey,
                        apiKey.keyId,
                        apiKey.permissions.map { SimpleGrantedAuthority(it) })
    }

    fun logout() {
        SecurityContextHolder.getContext().authentication = null
    }

    fun getTestPaths(subdir: String): List<Path> {
        val paths = Json.Mapper.readValue<List<String>>(File("src/test/resources/test-data/files.json"))
        return paths.filter {
            it.contains(subdir)
        }.map {
            Paths.get(it)
        }
    }

    fun getTestImagePath(subdir: String): Path {
        return Paths.get("/tmp/images/$subdir")
    }

    fun getTestAssets(subdir: String): List<Source> {

        val formats = setOf("jpg", "pdf", "m4v", "gif", "tif")

        val result = mutableListOf<Source>()
        val imagePaths = Json.Mapper.readValue<List<String>>(File("src/test/resources/test-data/files.json"))
        for (path in imagePaths) {
            if (!path.contains(subdir) || !formats.contains(FileUtils.extension(path).toLowerCase())) {
                continue
            }

            val f = File(path)
            val b = Source(f)
            // b.setAttr("test.path", getTestImagePath(subdir).toAbsolutePath().toString())
            b.setAttr("location.point", mapOf("lat" to "36.996460", "lon" to "-109.043360"))
            b.setAttr("location.state", "New Mexico")
            b.setAttr("location.country", "USA")
            b.setAttr("location.keywords", listOf("boring", "tourist", "attraction"))
            b.setAttr("media.width", 1024)
            b.setAttr("media.height", 1024)
            b.setAttr("media.title", "Picture of ${f.name}")
            val id = UUID.randomUUID().toString()
            val proxies = Lists.newArrayList<Proxy>()
            proxies.add(Proxy(width = 100, height = 100, id = "proxy___${id}_foo.jpg", mimetype = "image/jpeg"))
            proxies.add(Proxy(width = 200, height = 200, id = "proxy___${id}_bar.jpg", mimetype = "image/jpeg"))
            proxies.add(Proxy(width = 300, height = 300, id = "proxy___${id}_bing.jpg", mimetype = "image/jpeg"))

            val p = ProxySchema()
            p.proxies = proxies
            b.setAttr("proxies", p)
            result.add(b)
        }

        return result
    }

    fun addTestAssets(subdir: String) {
        addTestAssets(getTestAssets(subdir))
    }

    fun addTestVideoAssets() {
        val videoAssets = mutableListOf<Source>()
        val paths = Json.Mapper.readValue<List<String>>(File("src/test/resources/test-data/files.json"))

        for (path in paths) {
            if ("video/" !in path) {
                continue
            }
            val file = File(path)
            val source = Source(file)
            source.setAttr("test.path", file.toPath().toAbsolutePath().toString())
            val id = UUID.randomUUID().toString()
            val proxies = Lists.newArrayList<Proxy>()
            proxies.add(Proxy(width = 100, height = 100, id = "proxy___${id}_foo.jpg", mimetype = "image/jpeg"))
            proxies.add(Proxy(width = 200, height = 200, id = "proxy___${id}_bar.jpg", mimetype = "image/jpeg"))
            proxies.add(Proxy(width = 300, height = 300, id = "proxy___${id}_bing.jpg", mimetype = "image/jpeg"))
            proxies.add(Proxy(width = 1920, height = 1080, id = "proxy___${id}_transcode.mp4", mimetype = "video/mp4"))

            val proxySchema = ProxySchema()
            proxySchema.proxies = proxies
            source.setAttr("proxies", proxySchema)
            source.setAttr("proxy_id", id)
            videoAssets.add(source)
        }
        addTestAssets(videoAssets)
    }

    /**
     * Create a batch of test assets.
     *
     * @param builders: A list of Source objects describing the assets.
     * @param commitToDb: Set to true if the assets should be committed in a separate TX.
     *
     */
    fun addTestAssets(builders: List<Source>, commitToDb: Boolean = true) {
        for (source in builders) {

            logger.info("Adding test asset: {}", source.path.toString())
            source.setAttr(
                "source.keywords", ImmutableList.of(
                    source.sourceSchema.filename,
                    source.sourceSchema.extension
                )
            )

            val req = BatchCreateAssetsRequest(listOf(source)).apply { isUpload = true }

            if (commitToDb) {
                val tmpl = TransactionTemplate(transactionManager)
                tmpl.propagationBehavior = Propagation.REQUIRES_NEW.value()
                tmpl.execute(object : TransactionCallbackWithoutResult() {
                    override fun doInTransactionWithoutResult(transactionStatus: TransactionStatus) {
                        assetService.createOrReplaceAssets(req)
                    }
                })
            } else {
                assetService.createOrReplaceAssets(req)
            }
        }
        refreshIndex()
    }

    fun refreshIndex(sleep: Long = 0) {
        indexRoutingService.refreshAll()
    }
}
