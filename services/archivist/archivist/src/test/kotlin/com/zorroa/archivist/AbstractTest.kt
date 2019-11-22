package com.zorroa.archivist

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.collect.Lists
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.capture
import com.zorroa.archivist.clients.ApiKey
import com.zorroa.archivist.clients.AuthServerClient
import com.zorroa.archivist.clients.ZmlpUser
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.Project
import com.zorroa.archivist.domain.ProjectSpec
import com.zorroa.archivist.schema.Proxy
import com.zorroa.archivist.schema.ProxySchema
import com.zorroa.archivist.security.AnalystAuthentication
import com.zorroa.archivist.security.Role
import com.zorroa.archivist.service.AssetService
import com.zorroa.archivist.service.EsClientCache
import com.zorroa.archivist.service.FileServerProvider
import com.zorroa.archivist.service.IndexClusterService
import com.zorroa.archivist.service.IndexRoutingService
import com.zorroa.archivist.service.IndexService
import com.zorroa.archivist.service.PipelineService
import com.zorroa.archivist.service.ProjectService
import com.zorroa.archivist.service.TransactionEventManager
import com.zorroa.archivist.util.FileUtils
import com.zorroa.archivist.util.Json
import com.zorroa.archivist.util.randomString
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.client.Request
import org.elasticsearch.client.RequestOptions
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
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
    protected lateinit var projectService: ProjectService

    @Autowired
    protected lateinit var indexService: IndexService

    @Autowired
    protected lateinit var assetService: AssetService

    @Autowired
    protected lateinit var pipelineService: PipelineService

    @Autowired
    protected lateinit var esClientCache: EsClientCache

    @Autowired
    protected lateinit var properties: ApplicationProperties

    @Autowired
    internal lateinit var indexRoutingService: IndexRoutingService

    @Autowired
    internal lateinit var indexClusterService: IndexClusterService

    @Autowired
    internal lateinit var transactionEventManager: TransactionEventManager

    @MockBean
    lateinit var authServerClient: AuthServerClient

    protected lateinit var jdbc: JdbcTemplate

    protected lateinit var resources: Path

    protected lateinit var project: Project

    init {
        ArchivistConfiguration.unittest = true
        Json.Mapper.registerModule(KotlinModule())
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

        setupElastic()

        /**
         * If ES is required, cleanup the indexes.
         */
        if (requiresElasticSearch()) {
            cleanElastic()
        }

        /**
         * Setup a test project.
         */
        project = projectService.create(
            ProjectSpec(
                "unittest",
                projectId = UUID.fromString("00000000-0000-0000-0000-000000000000")
            )
        )

        // TODO: Remove this once the indexRouteService can manage states.
        // Set the project index route to the current state
        jdbc.update("UPDATE index_route SET int_state=0")

        /**
         * Setup mocks for calls out to the authentication service.
         */
        setupAuthServerMocks()

        /**
         * Ensures that all transaction events run within the unit test transaction.
         * If this was not set then  transaction events like AfterCommit would never execute
         * because unit test transactions are never committed.
         */
        transactionEventManager.isImmediateMode = true

        /**
         * Setup authentication.
         */
        authenticate()
    }

    fun authenticateAsAnalyst() {
        SecurityContextHolder.getContext().authentication =
            AnalystAuthentication("https://127.0.0.1:5000")
    }

    fun setupAuthServerMocks() {
        /**
         * Stub out network calls to the authServerClient.
         */
        val proj = ArgumentCaptor.forClass(Project::class.java)
        val permissions = ArgumentCaptor.forClass(listOf("foo").javaClass)

        // Create ApiKey
        Mockito.`when`(
            authServerClient.createApiKey(
                capture<Project>(proj),
                any(),
                capture<List<String>>(permissions)
            )
        ).then {
            ApiKey(
                UUID.randomUUID(),
                proj.value.id,
                randomString(64)
            )
        }

        // Get ApiKey
        Mockito.`when`(
            authServerClient.getApiKey(
                any(), any()
            )
        ).then {
            ApiKey(
                UUID.randomUUID(),
                project.id,
                randomString(64)
            )
        }
    }

    fun setupElastic() {
        val cluster = indexClusterService.createDefaultCluster()
        jdbc.update("UPDATE index_cluster SET int_state=1 WHERE pk_index_cluster=?", cluster.id)
    }

    fun cleanElastic() {
        val clusterUrl = properties.getString("archivist.es.url")
        try {
            val rest = esClientCache.getRestHighLevelClient(clusterUrl)
            val reqDel = DeleteIndexRequest("_all")
            rest.indices().delete(reqDel, RequestOptions.DEFAULT)
        } catch (e: Exception) {
            logger.warn("Failed to delete test index, this is usually ok.", e)
        }
    }

    fun refreshElastic() {
        val cluster = indexClusterService.createDefaultCluster()
        val client = indexClusterService.getRestHighLevelClient(cluster).lowLevelClient
        val req = Request("POST", "/_refresh")
        client.performRequest(req)
    }

    /**
     * Authenticates a user as admin but with all permissions, including internal ones.
     */
    fun authenticate() {
        val user = ZmlpUser(
            UUID.fromString("00000000-0000-0000-0000-000000000000"),
            project.id,
            "unittest-key",
            listOf(Role.PROJADMIN)
        )

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                user,
                user.permissions.map { SimpleGrantedAuthority(it) })
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

    fun getTestAssets(subdir: String): List<TestAsset> {

        val formats = setOf("jpg", "pdf", "m4v", "gif", "tif")

        val result = mutableListOf<TestAsset>()
        val imagePaths = Json.Mapper.readValue<List<String>>(File("src/test/resources/test-data/files.json"))
        for (path in imagePaths) {
            if (!path.contains(subdir) || !formats.contains(FileUtils.extension(path).toLowerCase())) {
                continue
            }

            val f = File(path)
            val b = TestAsset(f)
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
        val videoAssets = mutableListOf<TestAsset>()
        val paths = Json.Mapper.readValue<List<String>>(File("src/test/resources/test-data/files.json"))

        for (path in paths) {
            if ("video/" !in path) {
                continue
            }
            val file = File(path)
            val source = TestAsset(file) // file
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
    fun addTestAssets(builders: List<TestAsset>, commitToDb: Boolean = true) {
        logger.info("addiing test assets2")
        for (source in builders) {
            logger.info("Adding test asset: {}", source.path.toString())
            source.setAttr(
                "source.keywords", listOf(
                    source.getAttr<String>("source.filename"),
                    source.getAttr<String>("source.extension")
                )
            )
            val req = BatchCreateAssetsRequest(listOf(source)).apply { isUpload = true }
            assetService.createOrReplaceAssets(req)
        }
        refreshIndex()
    }

    fun refreshIndex(sleep: Long = 0) {
        refreshElastic()
    }
}
