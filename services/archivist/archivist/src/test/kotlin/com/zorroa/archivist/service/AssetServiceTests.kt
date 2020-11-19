package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.AssetMetrics
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchUploadAssetsRequest
import com.zorroa.archivist.domain.Label
import com.zorroa.archivist.domain.FileExtResolver
import com.zorroa.archivist.domain.InternalTask
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.ModelSpec
import com.zorroa.archivist.domain.ModelType
import com.zorroa.archivist.domain.ProcessorMetric
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ProjectFileLocator
import com.zorroa.archivist.domain.ProjectStorageCategory
import com.zorroa.archivist.domain.ProjectStorageEntity
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.archivist.domain.TaskState
import com.zorroa.archivist.domain.UpdateAssetLabelsRequest
import com.zorroa.archivist.domain.UpdateAssetRequest
import com.zorroa.archivist.domain.emptyZpsScript
import com.zorroa.archivist.domain.emptyZpsScripts
import com.zorroa.archivist.repository.ProjectQuotasDao
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.storage.ProjectStorageService
import com.zorroa.archivist.util.FileUtils
import com.zorroa.zmlp.util.Json
import org.elasticsearch.client.ResponseException
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.mock.web.MockMultipartFile
import java.io.File
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AssetServiceTests : AbstractTest() {

    @Autowired
    lateinit var pipelineModuleService: PipelineModService

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var modelService: ModelService

    @Autowired
    lateinit var dispatcherService: DispatcherService

    @Autowired
    lateinit var projectStorageService: ProjectStorageService

    @Autowired
    lateinit var projectQuotasDao: ProjectQuotasDao

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Test
    fun testGetExistingAssetIdImage() {
        val spec = AssetSpec("gs://cats/large-brown-cat.jpg")
        assertNull(assetService.getExistingAssetId(spec))

        val req = BatchCreateAssetsRequest(
            assets = listOf(spec)
        )
        assetService.batchCreate(req)
        val assetId = assetService.getExistingAssetId(spec)
        assertNotNull(assetId)
        assertEquals("jvfC_RFfhDKDXgqmgTFdtszLXj15uP-3", assetId)
    }

    @Test
    fun testGetExistingAssetIdDoc() {
        val spec = AssetSpec("gs://cats/large-brown-cat.pdf", page = 5)
        assertNull(assetService.getExistingAssetId(spec))

        val req = BatchCreateAssetsRequest(
            assets = listOf(spec)
        )
        assetService.batchCreate(req)
        assertNotNull(assetService.getExistingAssetId(spec))
    }

    @Test
    fun testGetExistingAssetIdById() {
        val spec = AssetSpec("gs://cats/large-brown-cat.pdf", id = "abcd12345")
        assertNull(assetService.getExistingAssetId(spec))

        val req = BatchCreateAssetsRequest(
            assets = listOf(spec)
        )
        assetService.batchCreate(req)
        assertNotNull(assetService.getExistingAssetId(spec))
        assertTrue(assetService.getAll(listOf("abcd12345")).isNotEmpty())
    }

    @Test
    fun testBatchCreateAssetsWithModule() {
        pipelineModuleService.updateStandardMods()

        val req = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg")),
            modules = listOf("zvi-label-detection")
        )

        val rsp = assetService.batchCreate(req)
        val jobId = rsp.jobId

        val tasks = jobService.getTasks(jobId!!)
        val script = jobService.getZpsScript(tasks[0].id)

        // Check the module was applied.
        assertEquals(
            "zmlp_analysis.zvi.ZviLabelDetectionProcessor",
            script.execute!!.last().className
        )
    }

    @Test(expected = DataRetrievalFailureException::class)
    fun testBatchCreateAssets_moduleNotFound() {
        pipelineModuleService.updateStandardMods()

        val req = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg")),
            modules = listOf("zmlp-arg!")
        )

        assetService.batchCreate(req)
    }

    @Test
    fun testBatchCreateAssetsWithTask() {
        val req = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg")),
            task = InternalTask(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "test",
                TaskState.Success
            )
        )

        val rsp = assetService.batchCreate(req)
        assertEquals(1, rsp.created.size)

        val asset = assetService.getAsset(rsp.created[0])
        assertEquals(req.assets[0].uri, asset.getAttr("source.path", String::class.java))
        assertNotNull(asset.getAttr("system.jobId"))
        assertNotNull(asset.getAttr("system.dataSourceId"))
    }

    @Test
    fun testBatchCreateAssets_failInvalidDynamicField() {
        val spec = AssetSpec(
            "gs://cats/large-brown-cat.jpg",
            mapOf("dog" to "cat")
        )

        val req = BatchCreateAssetsRequest(
            assets = listOf(spec)
        )
        val rsp = assetService.batchCreate(req)
        assertEquals(1, rsp.failed.size)
        assertTrue(rsp.failed[0].message.contains("is not allowed"))
    }

    @Test
    fun testBatchCreateAssets_WithAuxFields() {
        val spec = AssetSpec(
            "gs://cats/large-brown-cat.jpg",
            mapOf("aux.pet" to "dog")
        )

        val req = BatchCreateAssetsRequest(
            assets = listOf(spec)
        )
        val rsp = assetService.batchCreate(req)
        assertTrue(rsp.failed.isEmpty())
        val assets = assetService.getAll(rsp.created)
        assertEquals("dog", assets[0].getAttr("aux.pet", String::class.java))
    }

    @Test
    fun testBatchCreateNonPageableWithPage() {
        val spec = AssetSpec(
            "gs://cats/large-brown-cat.jpg",
            page = 2
        )

        val req = BatchCreateAssetsRequest(
            assets = listOf(spec)
        )
        val rsp = assetService.batchCreate(req)
        val assets = assetService.getAll(rsp.created)

        assertNull(assets[0].getAttr("media.pageNumber"))
        assertNull(assets[0].getAttr("media.pageStack"))
    }

    @Test
    fun testBatchCreateVideo() {
        val spec = AssetSpec(
            "gs://cats/large-brown-cat.mp4",
            mapOf("system.hello" to "foo")
        )

        val req = BatchCreateAssetsRequest(
            assets = listOf(spec)
        )
        val rsp = assetService.batchCreate(req)
        val assets = assetService.getAll(rsp.created)
        Json.prettyPrint(assets)
    }

    @Test
    fun testGetExistingAssetIds() {
        AssetSpec(
            "gs://cats/large-brown-cat.jpg",
            mapOf("system.hello" to "foo"),
        )

        val req = BatchCreateAssetsRequest(
            assets = listOf(
                AssetSpec(
                    "gs://cats/large-brown-cat.jpg"
                ),
                AssetSpec(
                    "gs://cats/large-brown-dog.png",
                    page = 2
                )
            )
        )
        val rsp = assetService.batchCreate(req)
        val assets = assetService.getAll(rsp.created)

        assertEquals(2, assetService.getExistingAssetIds(assets).size)
        assertEquals(1, assetService.getExistingAssetIds(listOf(assets[0])).size)

        val fake = Asset("abc123")
        fake.setAttr("source.path", "/foo")
        assertEquals(0, assetService.getExistingAssetIds(listOf(fake)).size)
    }

    @Test
    fun testBatchCreateAssetsWithPage() {
        val spec = AssetSpec(
            "gs://cats/large-brown-cat.pdf",
            page = 2
        )

        val req = BatchCreateAssetsRequest(
            assets = listOf(spec)
        )
        val rsp = assetService.batchCreate(req)
        val assets = assetService.getAll(rsp.created)

        assertEquals(2, assets[0].getAttr("media.pageNumber"))
        assertEquals("YJDNyJHRvJV2umLKQTvxiWuvVgBsIS8M", assets[0].getAttr("media.pageStack"))
    }

    @Test
    fun testBatchCreateAssetsWithLabel() {
        val ds = modelService.createModel(
            ModelSpec(
                "THB Characters",
                ModelType.ZVI_LABEL_DETECTION,
                moduleName = "thb-chars"
            )
        )

        val spec = AssetSpec(
            "gs://cats/large-brown-cat.jpg",
            label = Label(ds.id, "bilbo")
        )

        val req = BatchCreateAssetsRequest(
            assets = listOf(spec)
        )
        val rsp = assetService.batchCreate(req)
        val asset = assetService.getAll(rsp.created)[0]

        assertTrue(asset.attrExists("labels"))
        val labels = asset.getAttr("labels", Label.LIST_OF) ?: listOf()
        assertEquals(1, labels.size)
        assertEquals("bilbo", labels[0].label)
    }

    /**
     * Recreating an asset that already exists should fail.
     */
    @Test
    fun testBatchCreateAssets_alreadyExists() {
        val req = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg"))
        )

        assetService.batchCreate(req)
        val rsp = assetService.batchCreate(req)
        assertEquals(1, rsp.exists.size)
        assertEquals(0, rsp.created.size)
        assertEquals(0, rsp.failed.size)
    }

    @Test
    fun testBatchDelete() {
        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg"))
        )
        val assetId = assetService.batchCreate(batchCreate).created[0]
        val loc = ProjectFileLocator(ProjectStorageEntity.ASSETS, assetId, ProjectStorageCategory.SOURCE, "bob.txt")
        val spec = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())
        projectStorageService.store(spec)

        val rsp = assetService.batchDelete(setOf(assetId))
        assertTrue(assetId in rsp.deleted)
        assertTrue(rsp.failed.isEmpty())
    }

    @Test
    fun tesUpdateAsset() {
        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg"))
        )
        val assetId = assetService.batchCreate(batchCreate).created[0]

        assetService.update(
            assetId,
            UpdateAssetRequest(mapOf("source" to mapOf("mimetype" to "cat")))
        )
        var asset = assetService.getAsset(assetId)
        assertEquals("cat", asset.getAttr("source.mimetype", String::class.java))
    }

    @Test(expected = ResponseException::class)
    fun testUpdateAssets_failOnDoesNotExist() {
        assetService.update(
            "abc",
            UpdateAssetRequest(mapOf("source" to mapOf("mimetype" to "cat")))
        )
    }

    @Test(expected = ResponseException::class)
    fun testUpdateAssets_failInvalidFieldType() {
        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg"))
        )
        val assetId = assetService.batchCreate(batchCreate).created[0]
        assetService.update(
            assetId,
            UpdateAssetRequest(mapOf("source" to mapOf("filename" to mapOf("cat" to "dog"))))
        )
    }

    @Test
    fun testBatchUpdateAssets() {
        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(
                AssetSpec("gs://cats/large-brown-cat.jpg"),
                AssetSpec("gs://cats/cat-movie.m4v")
            )
        )
        val createRsp = assetService.batchCreate(batchCreate)
        val asset1 = assetService.getAsset(createRsp.created[0])
        val asset2 = assetService.getAsset(createRsp.created[1])

        val update = mapOf(
            asset1.id to UpdateAssetRequest(
                mapOf("aux" to mapOf("foo" to "bar"))
            ),
            asset2.id to UpdateAssetRequest(
                mapOf("source" to mapOf("mimetype" to "cat"))
            )
        )
        val rsp = assetService.batchUpdate(update)
        assertFalse(rsp.hasFailures())
    }

    @Test
    fun testBatchUpdateAssets_failOnDoesNotExist() {
        val update = mapOf(
            "abc" to UpdateAssetRequest(
                mapOf("aux" to mapOf("foo" to "bar"))
            ),
            "123" to UpdateAssetRequest(
                mapOf("source" to mapOf("mimetype" to "cat"))
            )
        )
        val rsp = assetService.batchUpdate(update)
        assertTrue("document missing" in rsp.items[0].failureMessage)
        assertTrue("document missing" in rsp.items[1].failureMessage)
        assertTrue(rsp.hasFailures())
    }

    @Test
    fun tesIndexAsset() {
        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg"))
        )
        val createRsp = assetService.batchCreate(batchCreate)
        var asset = assetService.getAsset(createRsp.created[0])
        asset.setAttr("aux.field", 1)

        assetService.index(asset.id, asset.document)
        asset = assetService.getAsset(createRsp.created[0])
        assertEquals("gs://cats/large-brown-cat.jpg", asset.getAttr("source.path", String::class.java))
        assertEquals(1, asset.getAttr("aux.field", Int::class.java))
    }

    @Test
    fun testBatchIndexAssets() {
        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg"))
        )
        val createRsp = assetService.batchCreate(batchCreate)
        val asset = assetService.getAll(createRsp.created)[0]
        asset.setAttr("aux.field", 1)

        val batchIndex = mapOf(asset.id to asset.document)
        val indexRsp = assetService.batchIndex(batchIndex)
        assertTrue(indexRsp.failed.isEmpty())
    }

    @Test
    fun testBatchIndexAssetsWithProjectCounters() {
        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(
                AssetSpec("gs://cats/large-brown-cat.jpg"),
                AssetSpec("gs://cats/large-brown-cat.mov"),
                AssetSpec("gs://cats/large-brown-cat.pdf")
            )
        )

        val createRsp = assetService.batchCreate(batchCreate)
        val assets = assetService.getAll(createRsp.created)
        val map = mutableMapOf<String, MutableMap<String, Any>>()

        assets.forEach {
            val ext = FileUtils.extension(it.getAttr<String>("source.path"))
            it.setAttr("aux.field", 1)
            it.setAttr("media.type", FileExtResolver.getType(ext))
            it.setAttr("media.length", 10.732)
            it.setAttr("clip.timeline", "full")
            it.setAttr("clip.type", "scene")
            it.setAttr("clip.start", 0)
            it.setAttr("clip.stop", 10.732)
            map[it.id] = it.document
        }

        val indexRsp = assetService.batchIndex(map, true)
        assertTrue(indexRsp.failed.isEmpty())

        val counts = jdbc.queryForMap("SELECT * FROM project_quota")
        assertEquals(BigDecimal("10.73"), counts["float_video_seconds"])
        assertEquals(2L, counts["int_page_count"])

        val time = jdbc.queryForMap("SELECT * FROM project_quota_time_series WHERE int_video_file_count > 0 LIMIT 1")
        assertEquals(1L, time["int_video_file_count"])
        assertEquals(1L, time["int_document_file_count"])
        assertEquals(1L, time["int_image_file_count"])
        assertEquals(BigDecimal("10.73"), counts["float_video_seconds"])
        assertEquals(2L, time["int_page_count"])
        assertEquals(1L, time["int_video_clip_count"])
    }

    @Test
    fun testBatchDeleteWithProjectCounters() {
        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(
                AssetSpec("gs://cat/large-brown-cat.jpg"),
                AssetSpec("gs://cat/large-brown-cat.mov", attrs = mutableMapOf("media.length" to 10.732)),
                AssetSpec("gs://cat/large-brown-cat.pdf")
            )
        )

        val createRsp = assetService.batchCreate(batchCreate)
        val assets = assetService.getAll(createRsp.created)
        val map = mutableMapOf<String, MutableMap<String, Any>>()

        assetService.batchDelete(createRsp.created.toSet())
        var quotaTimeSeries = jdbc.queryForMap(
            "SELECT * FROM project_quota_time_series WHERE int_deleted_image_file_count > 0 limit 1"
        )
        assertEquals(1L, quotaTimeSeries["int_deleted_image_file_count"])
        assertEquals(1L, quotaTimeSeries["int_deleted_document_file_count"])
        assertEquals(1L, quotaTimeSeries["int_deleted_video_file_count"])
        assertEquals(BigDecimal("10.73"), quotaTimeSeries["float_deleted_video_seconds"])
        assertEquals(1L, quotaTimeSeries["int_deleted_video_clip_count"])
        assertEquals(2L, quotaTimeSeries["int_deleted_page_count"])

        var quota = projectQuotasDao.getQuotas(getProjectId())
        assertEquals(2L, quota.deletedPageCount)
        assertEquals(BigDecimal("10.73"), quota.deletedVideoSecondsCount)
    }

    fun testBatchIndexAssetsWithoutMediaType() {
        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(
                AssetSpec("gs://cats/large-brown-cat.jpg"),
                AssetSpec("gs://cats/large-brown-cat.mov"),
                AssetSpec("gs://cats/large-brown-cat-no-type.jpg"),
                AssetSpec("gs://cats/large-brown-cat.pdf"),
                AssetSpec("gs://cats/large-brown-cat-no-type-noext")
            )
        )

        val createRsp = assetService.batchCreate(batchCreate)
        val assets = assetService.getAll(createRsp.created)
        val map = mutableMapOf<String, MutableMap<String, Any>>()

        assets.forEach {
            map[it.id] = it.document
        }

        val indexRsp = assetService.batchIndex(map, true)

        assertFalse(indexRsp.failed.isEmpty())
        assertEquals(4, indexRsp.indexed.size)
        assertEquals("gs://cats/large-brown-cat-no-type-noext", indexRsp.failed[0].uri)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBatchDeleteExceedMaxSize() {
        val maxBatchSize = properties.getInt("archivist.assets.deletion-max-batch-size")

        // Max + 1
        val batchCreate = BatchCreateAssetsRequest(
            assets =
                (0..maxBatchSize).map { AssetSpec("gs://cat/large-brown-cat-$it.jpg") }

        )
        val createRsp = assetService.batchCreate(batchCreate)
        assetService.batchDelete(createRsp.created.toSet())
    }

    @Test
    fun testBatchIndexAssetsWithTempFields() {
        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg"))
        )
        val createRsp = assetService.batchCreate(batchCreate)
        var asset = assetService.getAll(createRsp.created)[0]
        asset.setAttr("aux.field", 1)
        asset.setAttr("tmp.field", 1)

        val batchIndex = mapOf(asset.id to asset.document)
        val indexRsp = assetService.batchIndex(batchIndex)
        assertTrue(indexRsp.failed.isEmpty())

        asset = assetService.getAsset(createRsp.created[0])
        assertFalse(asset.attrExists("tmp.field"))
        assertFalse(asset.attrExists("tmp"))
    }

    @Test
    fun testBatchIndexAssetWithNoPageOne() {
        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.gif", page = 2))
        )
        val createRsp = assetService.batchCreate(batchCreate)
        var asset = assetService.getAsset(createRsp.created[0])

        // When there is no page 1 we figure out what page one's pageStack is.
        assertEquals("PplipeapbU0tMnFxvZ225hGQKSMvjuyX", asset.getAttr("media.pageStack"))
        assertEquals(2, asset.getAttr("media.pageNumber"))
        assertEquals("YDWpJcJgss-FXM6ydFE1BuAent7Q6fUu", asset.id)
    }

    /**
     * Trying to update assets that don't exist should fail.
     */
    @Test(expected = IllegalArgumentException::class)
    fun testBatchIndexAssets_failNotCreatedSingle() {
        val req = mapOf("foo" to mutableMapOf<String, Any>())
        val rsp = assetService.batchIndex(req)
        assertTrue(rsp.failed.isEmpty())
    }

    @Test
    fun testBatchUploadAssets() {
        val batchUpload = BatchUploadAssetsRequest(
            assets = listOf(AssetSpec("/foo/bar/toucan.jpg"))
        )

        batchUpload.files = arrayOf(
            MockMultipartFile(
                "files", "file-name.data", "image/jpeg",
                File("src/test/resources/test-data/toucan.jpg").inputStream().readBytes()
            )
        )

        val rsp = assetService.batchUpload(batchUpload)
        val assets = assetService.getAll(rsp.created)
        assertEquals("toucan.jpg", assets[0].getAttr("source.filename", String::class.java))
        assertEquals(1582911032, assets[0].getAttr("source.checksum", Int::class.java))
        assertEquals(97221, assets[0].getAttr("source.filesize", Long::class.java))
        assertTrue(rsp.failed.isEmpty())
    }

    @Test
    fun testDerivePageFromExistingAsset() {

        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/cat-info.pdf"))
        )

        val sourceAsset = assetService.getAsset(assetService.batchCreate(batchCreate).created[0])
        val spec = AssetSpec(
            "asset:${sourceAsset.id}",
            page = 5
        )

        val newAsset = Asset()
        assetService.derivePage(newAsset, spec)

        assertEquals("5kQEjlZwTrH3ABa9JItyqlakor22j_lF", sourceAsset.id)
        assertEquals("5kQEjlZwTrH3ABa9JItyqlakor22j_lF", sourceAsset.getAttr("media.pageStack"))
        assertEquals("5kQEjlZwTrH3ABa9JItyqlakor22j_lF", newAsset.getAttr("media.pageStack"))
        assertEquals(5, newAsset.getAttr("media.pageNumber"))
    }

    @Test
    fun testDerivePageFromFilePath() {
        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(
                AssetSpec(
                    "gs://cats/cat-stuff.pdf", page = 2
                )
            )
        )
        val sourceAsset = assetService.getAsset(assetService.batchCreate(batchCreate).created[0])
        assertEquals("EJvwFoCQ-ARK30i3bXJRIfW9gRfOXMn9", sourceAsset.id)
        assertEquals("X45qGhIXYgiFcqOn0KuQxUG6ojZrRPuM", sourceAsset.getAttr("media.pageStack"))
        assertEquals(2, sourceAsset.getAttr("media.pageNumber"))
    }

    @Test
    fun needsAssetReprocessing() {
        val asset = Asset()
        // no metadata would signal yes, reprocess
        assertTrue(assetService.assetNeedsReprocessing(asset, listOf()))

        // Empty pipeline, no need to reprocess
        var metrics = AssetMetrics(listOf(ProcessorMetric("foo.Bar", "bing", 1, null)))
        asset.setAttr("metrics", metrics)
        assertFalse(assetService.assetNeedsReprocessing(asset, listOf()))

        // Processor has an error
        metrics = AssetMetrics(listOf(ProcessorMetric("foo.Bar", "bing", 1, "fatal")))
        asset.setAttr("metrics", metrics)
        assertTrue(assetService.assetNeedsReprocessing(asset, listOf()))

        // No processing, checksum match
        metrics = AssetMetrics(listOf(ProcessorMetric("foo.Bar", "bing", 281740160, null)))
        asset.setAttr("metrics", metrics)
        val ref = ProcessorRef("foo.Bar", "core")
        assertFalse(assetService.assetNeedsReprocessing(asset, listOf(ref)))

        // processing, checksum does not match
        metrics = AssetMetrics(listOf(ProcessorMetric("foo.Bar", "bing", 1, null)))
        asset.setAttr("metrics", metrics)
        assertTrue(assetService.assetNeedsReprocessing(asset, listOf(ref)))
    }

    @Test
    fun testCreateAnalysisTask() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScripts("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        // setup a job
        jobService.create(spec)
        val zps = emptyZpsScript("bar")
        zps.execute = mutableListOf(ProcessorRef("foo", "bar"))

        // Add an asset to make a test for
        val req = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg"))
        )
        val rsp = assetService.batchCreate(req)

        // Create an expand
        val task1 = dispatcherService.getWaitingTasks(getProjectId(), 1)
        val newTask = assetService.createAnalysisTask(task1[0], rsp.created, listOf("abc123"))
        assertEquals("Expand with 1 assets, 0 processors.", newTask?.name)
    }

    @Test
    fun testUpdateLabels() {
        val ds = modelService.createModel(ModelSpec("test", ModelType.ZVI_LABEL_DETECTION))
        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/cat-movie.m4v"))
        )
        // Add a label.
        var asset = assetService.getAsset(assetService.batchCreate(batchCreate).created[0])
        assetService.updateLabels(
            UpdateAssetLabelsRequest(
                // Validate adding 2 identical labels only adds 1
                mapOf(
                    asset.id to listOf(
                        Label(ds.id, "cat", simhash = "12345"),
                        Label(ds.id, "cat", simhash = "12345")
                    )
                )
            )
        )

        asset = assetService.getAsset(asset.id)
        var labels = asset.getAttr("labels", Label.LIST_OF)
        assertEquals(1, labels?.size)
        assertEquals("cat", labels?.get(0)?.label)
        assertEquals("12345", labels?.get(0)?.simhash)

        // Remove a label
        assetService.updateLabels(
            UpdateAssetLabelsRequest(
                null,
                mapOf(asset.id to listOf(Label(ds.id, "cat")))
            )
        )

        asset = assetService.getAsset(asset.id)
        labels = asset.getAttr("labels", Label.LIST_OF) ?: listOf()
        assert(labels.isNullOrEmpty())
    }
}
