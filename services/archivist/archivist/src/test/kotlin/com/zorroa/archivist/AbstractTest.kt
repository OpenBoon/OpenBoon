package com.zorroa.archivist

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.capture
import com.nhaarman.mockito_kotlin.whenever
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.Project
import com.zorroa.archivist.domain.ProjectSpec
import com.zorroa.archivist.security.AnalystAuthentication
import com.zorroa.archivist.security.getAuthentication
import com.zorroa.archivist.service.AssetService
import com.zorroa.archivist.service.ClusterBackupService
import com.zorroa.archivist.service.EsClientCache
import com.zorroa.archivist.service.IndexClusterService
import com.zorroa.archivist.service.IndexRoutingService
import com.zorroa.archivist.service.PipelineService
import com.zorroa.archivist.service.ProjectService
import com.zorroa.archivist.service.TransactionEventManager
import com.zorroa.archivist.util.FileUtils
import com.zorroa.zmlp.util.Json
import com.zorroa.archivist.util.randomString
import com.zorroa.zmlp.apikey.ApiKey
import com.zorroa.zmlp.apikey.AuthServerClient
import com.zorroa.zmlp.apikey.Permission
import com.zorroa.zmlp.apikey.SigningKey
import com.zorroa.zmlp.apikey.ZmlpActor
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.client.Request
import org.elasticsearch.client.RequestOptions
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
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
@ActiveProfiles("test", "aws")
@WebAppConfiguration
@Transactional
abstract class AbstractTest {

    val logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    protected lateinit var projectService: ProjectService

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

    @Autowired
    internal lateinit var clusterBackupService: ClusterBackupService

    @MockBean
    lateinit var authServerClient: AuthServerClient

    protected lateinit var jdbc: JdbcTemplate

    protected lateinit var resources: Path

    protected lateinit var project: Project

    protected val TEST_AWS_CREDS =
        """{"aws_access_key_id": "foo", "aws_secret_access_key": "kirkspockbones"}"""

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
         * Setup mocks for calls out to the authentication service.
         */
        setupAuthServerMocks()

        /**
         * Setup a test project.
         */
        project = projectService.create(
            ProjectSpec(
                "unittest",
                id = UUID.fromString("00000000-0000-0000-0000-000000000000")
            )
        )

        // TODO: Remove this once the indexRouteService can manage states.
        // Set the project index route to the current state
        jdbc.update("UPDATE index_route SET int_state=0")

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
            AnalystAuthentication("http://127.0.0.1:5000", "unittest")
    }

    fun setupAuthServerMocks() {
        /**
         * Stub out network calls to the authServerClient.
         */
        val proj = ArgumentCaptor.forClass(UUID::class.java)
        val permissions = ArgumentCaptor.forClass(setOf(Permission.AssetsImport).javaClass)

        // Create ApiKey
        whenever(
            authServerClient.createApiKey(
                capture<UUID>(proj),
                any(),
                capture<Set<Permission>>(permissions)
            )
        ).then {
            ApiKey(
                UUID.randomUUID(),
                proj.value,
                randomString(24),
                randomString(64),
                "key-name",
                permissions.value
            )
        }

        // Get ApiKey
        whenever(
            authServerClient.getSigningKey(
                any(), any()
            )
        ).then {
            SigningKey(
                randomString(24),
                randomString(64)
            )
        }

        // Setup a inception key first.
        val actor = ZmlpActor(
            UUID.fromString("00000000-0000-0000-0000-000000000000"),
            UUID.fromString("00000000-0000-0000-0000-000000000000"),
            "inception-key",
            setOf()
        )
        SecurityContextHolder.getContext().authentication = actor.getAuthentication()
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
        authenticate(project.id)
    }

    fun authenticate(project: UUID) {
        val actor = ZmlpActor(
            UUID.fromString("00000000-0000-0000-0000-000000000000"),
            project,
            "unittest-key",
            Permission.values().toSet()
        )
        SecurityContextHolder.getContext().authentication = actor.getAuthentication()
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

    fun getTestAssets(subdir: String): List<AssetSpec> {

        val formats = setOf("jpg", "pdf", "m4v", "gif", "tif")
        val imagePaths = Json.Mapper.readValue<List<String>>(
            File("src/test/resources/test-data/files.json")
        )

        return imagePaths.mapNotNull { path ->
            if (!path.contains(subdir) || !formats.contains(FileUtils.extension(path).toLowerCase())) {
                null
            }
            val asset = AssetSpec(path)
            asset.attrs = mapOf(
                "media" to mapOf(
                    "width" to 1024,
                    "height" to 1024,
                    "title" to "Picture of $path"
                )
            )
            asset
        }
    }

    fun addTestAssets(subdir: String) {
        addTestAssets(getTestAssets(subdir))
    }

    fun addTestAssets(assets: List<AssetSpec>) {
        val req = BatchCreateAssetsRequest(assets)
        assetService.batchCreate(req)
        refreshIndex()
    }

    fun refreshIndex(sleep: Long = 0) {
        refreshElastic()
    }
}
