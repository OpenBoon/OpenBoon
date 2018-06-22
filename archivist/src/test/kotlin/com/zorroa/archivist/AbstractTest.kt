package com.zorroa.archivist

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Lists
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.UserSpec
import com.zorroa.archivist.sdk.security.UserRegistryService
import com.zorroa.archivist.security.UnitTestAuthentication
import com.zorroa.archivist.service.*
import com.zorroa.common.domain.Document
import com.zorroa.common.domain.Source
import com.zorroa.common.schema.Proxy
import com.zorroa.common.schema.ProxySchema
import com.zorroa.common.util.FileUtils
import com.zorroa.common.util.Json
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.client.RestHighLevelClient
import org.junit.Before
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
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
@TestPropertySource("/test.properties")
@WebAppConfiguration
@Transactional
open abstract class AbstractTest {

    val logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    protected lateinit var client: RestHighLevelClient

    @Autowired
    protected lateinit var userService: UserService

    @Autowired
    protected lateinit var assetService: AssetService

    @Autowired
    protected lateinit var storageService: StorageService

    @Autowired
    protected lateinit var permissionService: PermissionService

    @Autowired
    protected lateinit var folderService: FolderService

    @Autowired
    protected lateinit var searchService: SearchService

    @Autowired
    protected lateinit var fieldService: FieldService

    @Autowired
    protected lateinit var indexService: IndexService

    @Autowired
    protected lateinit var properties: ApplicationProperties

    @Autowired
    protected lateinit var settingsService: SettingsService

    @Autowired
    protected lateinit var emailService: EmailService

    @Autowired
    protected lateinit var requestService: RequestService

    @Autowired
    protected lateinit var userRegistryService: UserRegistryService

    @Autowired
    internal lateinit var authenticationManager: AuthenticationManager

    @Autowired
    internal lateinit var transactionEventManager: TransactionEventManager

    @Autowired
    internal lateinit var transactionManager: DataSourceTransactionManager

    @Value("\${archivist.organization.single-org-index}")
    protected lateinit var alias: String

    protected lateinit var jdbc: JdbcTemplate

    protected lateinit var resources: Path

    init {
        ArchivistConfiguration.unittest = true
    }

    @Autowired
    fun setDataSource(dataSource: DataSource) {
        this.jdbc = JdbcTemplate(dataSource)
    }

    @Before
    @Throws(IOException::class)
    fun setup() {

        /*
          * Now that folders are created using what is essentially a nested transaction,
          * we can't rely on the unittests to roll them back.  For this, we manually delete
          * every folder not created by the SQL schema migration.
          *
          * Eventually we need a different way to do this because it relies on the created
          * time, which just happens to be the same for all schema created folders.
          *
          * //TODO: find a more robust way to handle deleting folders created by a test.
          * Maybe use naming conventions (test_) or utilize a new field on the table.
          *
         */
        val tmpl = TransactionTemplate(transactionManager)
        tmpl.propagationBehavior = Propagation.NOT_SUPPORTED.ordinal
        tmpl.execute(object : TransactionCallbackWithoutResult() {
            override fun doInTransactionWithoutResult(transactionStatus: TransactionStatus) {
                jdbc.update("DELETE FROM folder WHERE time_created !=1450709321000")
            }
        })

        /*
         * Ensures that all transaction events run within the unit test transaction.
         * If this was not set then  transaction events like AfterCommit would never execute
         * because unit test transactions are never committed.
         */
        transactionEventManager.isImmediateMode = true

        /**
         * Elastic must be created and cleaned before authentication.
         */
        cleanElastic()

        /**
         * Before we can do anything reliably we need a logged in user.
         */
        authenticate()

        val spec1 = UserSpec(
                "user",
                "user",
                "user@zorroa.com",
                firstName = "Bob",
                lastName = "User")

        userService.create(spec1)

        val spec2 = UserSpec(
                "librarian",
                "manager",
                "librarian@zorroa.com",
                firstName = "Anne",
                lastName = "Librarian")

        val manager = userService.create(spec2)
        userService.addPermissions(manager, listOf(
                permissionService.getPermission("zorroa::librarian")))


        resources = FileUtils.normalize(Paths.get("../../zorroa-test-data"))
        Json.Mapper.registerModule(KotlinModule())
    }

    fun testUserSpec(name: String="test") : UserSpec {
        return UserSpec(
                name,
                "test",
                "$name@zorroa.com",
                firstName = "mr",
                lastName = "test")
    }

    fun cleanElastic() {
        /*
         * The Elastic index(s) has been created, but we have to delete it and recreate it
         * so each test has a clean index.  Once this is done we can call setupDataSources()
         * which adds some standard data to both databases.
         */


        val reqDel = DeleteIndexRequest("_all")
        client.indices().delete(reqDel)

        val mapping = Json.Mapper.readValue<Map<String,Any>>(
                File("../elasticsearch/asset.json"), Json.GENERIC_MAP)

        val reqCreate = CreateIndexRequest("archivist")
        reqCreate.source(mapping)
        client.indices().create(reqCreate)
    }

    /**
     * Authenticates a user as admin but with all permissions, including internal ones.
     */
    fun authenticate() {
        val auth = UsernamePasswordAuthenticationToken("admin", "admin")
        SecurityContextHolder.getContext().authentication = authenticationManager.authenticate(auth)
    }

    fun authenticate(username: String) {
        authenticate(username, false)
    }

    fun authenticate(username: String, superUser: Boolean) {
        val authed = userRegistryService.getUser(username)
        val authorities = Lists.newArrayList(
                authed.authorities)

        if (superUser) {
            authorities.add(
                    permissionService.getPermission("zorroa::administrator"))
        }

        SecurityContextHolder.getContext().authentication = authenticationManager.authenticate(UnitTestAuthentication(authed, authorities))
    }

    fun logout() {
        SecurityContextHolder.getContext().authentication = null
    }

    fun getTestPath(subdir: String): Path {
        return resources.resolve(subdir)
    }

    fun getTestImagePath(subdir: String): Path {
        return if (subdir.startsWith("/")) {
            resources.resolve(subdir.substring(1))
        } else {
            resources.resolve("images/$subdir")
        }
    }

    fun getTestImagePath(): Path {
        return getTestImagePath("set04/standard")
    }

    private val SUPPORTED_FORMATS = ImmutableSet.of(
            "jpg", "pdf", "m4v", "gif", "tif")

    fun getTestAssets(subdir: String): List<Source> {
        val result = mutableListOf<Source>()
        val tip = getTestImagePath(subdir)
        for (f in tip.toFile().listFiles()!!) {

            if (f.isFile) {
                if (SUPPORTED_FORMATS.contains(FileUtils.extension(f.path).toLowerCase())) {
                    logger.info("adding test file: {}", f)
                    val b = Source(f)
                    b.setAttr("test.path", getTestImagePath(subdir).toAbsolutePath().toString())

                    val id = UUID.randomUUID().toString()

                    val proxies = Lists.newArrayList<Proxy>()
                    proxies.add(Proxy(width=100, height=100, id="proxy/" + id + "_foo.jpg", mimeType = "image/jpeg"))
                    proxies.add(Proxy(width=200, height=200, id="proxy/" + id + "_bar.jpg", mimeType = "image/jpeg"))
                    proxies.add(Proxy(width=300, height=300, id="proxy/" + id + "_bing.jpg", mimeType = "image/jpeg"))

                    val p = ProxySchema()
                    p.proxies = proxies
                    b.setAttr("proxies", p)
                    result.add(b)
                }
            }
        }

        for (f in getTestImagePath(subdir).toFile().listFiles()!!) {
            if (f.isDirectory) {
                result.addAll(getTestAssets(subdir + "/" + f.name))
            }
        }

        logger.info("TEST ASSET: {}", result)
        return result
    }

    fun addTestAssets(subdir: String) {
        addTestAssets(getTestAssets(subdir))
    }

    fun addTestAssets(builders: List<Source>) {
        for (source in builders) {
            val schema = source.sourceSchema

            logger.info("Adding test asset: {}", source.path.toString())
            source.setAttr("source.keywords", ImmutableList.of(
                    source.sourceSchema.filename,
                    source.sourceSchema.extension))
            indexService.index(source)
        }
        refreshIndex()
    }

    fun refreshIndex() {
        client.lowLevelClient.performRequest("POST", "/archivist/_refresh")
    }

    fun refreshIndex(sleep: Long) {
        client.lowLevelClient.performRequest("POST", "/archivist/_refresh")
    }
}
