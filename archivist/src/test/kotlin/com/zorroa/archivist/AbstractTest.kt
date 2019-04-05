package com.zorroa.archivist

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Lists
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.Source
import com.zorroa.archivist.domain.UserSpec
import com.zorroa.archivist.sdk.security.UserRegistryService
import com.zorroa.archivist.security.AnalystAuthentication
import com.zorroa.archivist.security.UnitTestAuthentication
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.service.*
import com.zorroa.archivist.util.FileUtils
import com.zorroa.common.schema.Proxy
import com.zorroa.common.schema.ProxySchema
import com.zorroa.common.util.Json
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.junit.Before
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import javax.sql.DataSource

@RunWith(SpringRunner::class)
@SpringBootTest
@TestPropertySource(locations=["classpath:test.properties"])
@WebAppConfiguration
@Transactional
open abstract class AbstractTest {

    val logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    protected lateinit var userService: UserService

    @Autowired
    protected lateinit var fileServerProvider: FileServerProvider

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
    protected lateinit var assetService: AssetService

    @Autowired
    protected lateinit var properties: ApplicationProperties

    @Autowired
    protected lateinit var settingsService: SettingsService

    @Autowired
    protected lateinit var emailService: EmailService

    @Autowired
    protected lateinit var requestService: RequestService

    @Autowired
    protected lateinit var organizationService: OrganizationService

    @Autowired
    protected lateinit var userRegistryService: UserRegistryService

    @Autowired
    internal lateinit var authenticationManager: AuthenticationManager

    @Autowired
    internal lateinit var indexRoutingService: IndexRoutingService

    @Autowired
    internal lateinit var fieldSystemService: FieldSystemService

    @Autowired
    internal lateinit var transactionEventManager: TransactionEventManager

    @Autowired
    internal lateinit var transactionManager: DataSourceTransactionManager

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

        /**
         * Clean out any committed data we left around for co-routihes and threads
         * to be tested.
         */
        val tmpl = TransactionTemplate(transactionManager)
        tmpl.propagationBehavior = Propagation.NOT_SUPPORTED.ordinal
        tmpl.execute(object : TransactionCallbackWithoutResult() {
            override fun doInTransactionWithoutResult(transactionStatus: TransactionStatus) {
                jdbc.update("DELETE FROM folder WHERE time_created !=1450709321000")
                jdbc.update("DELETE FROM asset")
                jdbc.update("DELETE FROM auditlog")
                jdbc.update("DELETE FROM cluster_lock")
                jdbc.update("UPDATE index_route SET str_index='unittest'")
            }
        })

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
        cleanElastic()
        setupDefaultOrganization()

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

    fun authenticateAsAnalyst() {
        SecurityContextHolder.getContext().authentication = AnalystAuthentication("https://127.0.0.1:5000")
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

        val rest = indexRoutingService.getEsRestClient()
        val reqDel = DeleteIndexRequest("unittest")

        /*
         * Delete will throw here if the index doesn't exist.
         */
        try {
            rest.client.indices().delete(reqDel)
        } catch (e: Exception) {
            logger.warn("Failed to delete 'unittest' index, this is usually ok.")
        }

        indexRoutingService.syncAllIndexRoutes()
    }

    fun setupDefaultOrganization() {
        val org = organizationService.get(getOrgId())
        fieldSystemService.setupDefaultFieldSets(org)
    }
    /**
     * Authenticates a user as admin but with all permissions, including internal ones.
     */
    fun authenticate() {
        val auth = UsernamePasswordAuthenticationToken("admin", "admin")

        val userAuthed = userRegistryService.getUser("admin")
        userAuthed.setAttr("company_id", "25274")
        SecurityContextHolder.getContext().authentication = UnitTestAuthentication(userAuthed, userAuthed.authorities)
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

        SecurityContextHolder.getContext().authentication =
                authenticationManager.authenticate(UnitTestAuthentication(authed, authorities))
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
                    b.setAttr("location.point", mapOf("lat" to "36.996460", "lon" to "-109.043360"))
                    b.setAttr("location.state", "New Mexico")
                    b.setAttr("location.country", "USA")
                    b.setAttr("location.keywords", listOf("boring", "tourist", "attraction"))
                    b.setAttr("media.width", 1024)
                    b.setAttr("media.height", 1024)
                    b.setAttr("media.title", "Picture of ${f.name}")
                    val id = UUID.randomUUID().toString()
                    val proxies = Lists.newArrayList<Proxy>()
                    proxies.add(Proxy(width=100, height=100, id="proxy___${id}_foo.jpg", mimetype = "image/jpeg"))
                    proxies.add(Proxy(width=200, height=200, id="proxy___${id}_bar.jpg", mimetype = "image/jpeg"))
                    proxies.add(Proxy(width=300, height=300, id="proxy___${id}_bing.jpg", mimetype = "image/jpeg"))

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

        return result
    }

    fun addTestAssets(subdir: String) {
        addTestAssets(getTestAssets(subdir))
    }

    fun addTestVideoAssets(subdir: String) {
        // note: does not recurse into subdirectories
        val videoAssets = mutableListOf<Source>()
        val path = resources.resolve(subdir)
        for (f in path.toFile().listFiles()!!) {

            if (f.isFile) {
                    logger.info("adding test file: {}", f)
                    val source = Source(f)
                    source.setAttr("test.path", path.toAbsolutePath().toString())
                    val id = UUID.randomUUID().toString()
                    val proxies = Lists.newArrayList<Proxy>()
                    proxies.add(Proxy(width=100, height=100, id="proxy___${id}_foo.jpg", mimetype = "image/jpeg"))
                    proxies.add(Proxy(width=200, height=200, id="proxy___${id}_bar.jpg", mimetype = "image/jpeg"))
                    proxies.add(Proxy(width=300, height=300, id="proxy___${id}_bing.jpg", mimetype = "image/jpeg"))
                    proxies.add(Proxy(width=1920, height=1080, id="proxy___${id}_transcode.mp4", mimetype = "video/mp4"))

                    val proxySchema = ProxySchema()
                    proxySchema.proxies = proxies
                    source.setAttr("proxies", proxySchema)
                    source.setAttr("proxy_id", id)
                    videoAssets.add(source)
            }
        }
        addTestAssets(videoAssets)
    }


    fun addTestAssets(builders: List<Source>) {
        for (source in builders) {
            val schema = source.sourceSchema

            logger.info("Adding test asset: {}", source.path.toString())
            source.setAttr("source.keywords", ImmutableList.of(
                    source.sourceSchema.filename,
                    source.sourceSchema.extension))

            /**
             * Co-routines and threads need to see committed data, so assets are committed for
             * that purpose but cleaned up before each test.
             */
            val tmpl = TransactionTemplate(transactionManager)
            tmpl.propagationBehavior = Propagation.REQUIRES_NEW.value()
            tmpl.execute(object : TransactionCallbackWithoutResult() {
                override fun doInTransactionWithoutResult(transactionStatus: TransactionStatus) {
                    assetService.batchCreateOrReplace(BatchCreateAssetsRequest(listOf(source)).apply { isUpload=true })
                }
            })

        }
        refreshIndex()
    }

    fun refreshIndex(sleep: Long=0) {
        indexRoutingService.refreshAll()
    }
}
