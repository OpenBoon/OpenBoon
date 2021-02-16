package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.Category
import boonai.archivist.domain.FileType
import boonai.archivist.domain.ModOp
import boonai.archivist.domain.ModOpType
import boonai.archivist.domain.ModelObjective
import boonai.archivist.domain.OpFilter
import boonai.archivist.domain.OpFilterType
import boonai.archivist.domain.PipelineModSpec
import boonai.archivist.domain.PipelineSpec
import boonai.archivist.domain.PipelineUpdate
import boonai.archivist.domain.ProcessorRef
import boonai.archivist.domain.Provider
import boonai.common.util.Json
import org.junit.Ignore
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.test.assertEquals

class PipelineResolverServiceTests : AbstractTest() {

    @Autowired
    lateinit var pipelineResolverService: PipelineResolverService

    @Autowired
    lateinit var pipelineModService: PipelineModService

    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Test
    fun testResolveModular() {
        pipelineModService.updateStandardMods()
        val pipeline = pipelineResolverService.resolveModular(
            listOf(
                "gcp-video-label-detection",
                "gcp-video-object-detection",
                "gcp-video-text-detection",
                "gcp-video-logo-detection",
                "gcp-video-explicit-detection",
                "gcp-speech-to-text"
            )
        )
        assertEquals(7, pipeline.execute.size)
    }

    @Test
    fun testResolveModularNoStandard() {
        pipelineModService.updateStandardMods()
        val pipeline = pipelineResolverService.resolveModular(
            listOf(
                "gcp-video-label-detection",
                "gcp-video-object-detection",
                "gcp-video-text-detection",
                "gcp-video-logo-detection",
                "gcp-video-explicit-detection",
                "gcp-speech-to-text"
            ),
            false
        )
        assertEquals(2, pipeline.execute.size)
    }

    @Test
    fun getStandardPipeline() {
        val pipeline = pipelineResolverService.getStandardPipeline()
        assertEquals("PrependMarker", pipeline.last().className)
    }

    @Test
    fun resolveUsingPipelineName() {
        pipelineModService.updateStandardMods()

        val pspec = PipelineSpec("test", modules = listOf("zvi-object-detection"))
        val pipeline = pipelineService.create(pspec)

        val resolved = pipelineResolverService.resolve(pipeline.name, null)
        val last = resolved.execute.last()
        assertEquals("boonai_analysis.zvi.ZviObjectDetectionProcessor", last.className)
        assertEquals("boonai/plugins-analysis", last.image)
    }

    @Test
    fun resolveUsingPipelineNameAndPlusModules() {
        pipelineModService.updateStandardMods()

        val pspec = PipelineSpec("test", modules = listOf("zvi-object-detection"))
        val pipeline = pipelineService.create(pspec)

        val rpipeline = pipelineResolverService.resolve(pipeline.name, listOf("zvi-label-detection"))
        val resolved = rpipeline.execute
        val last = resolved.last()
        assertEquals(last.className, "boonai_analysis.zvi.ZviLabelDetectionProcessor")
        assertEquals(last.image, "boonai/plugins-analysis")

        val beforeLast = resolved[resolved.size - 2]
        assertEquals("boonai_analysis.zvi.ZviObjectDetectionProcessor", beforeLast.className)
        assertEquals("boonai/plugins-analysis", beforeLast.image)
    }

    @Test
    fun resolveAndCheckGlobals() {
        pipelineModService.updateStandardMods()

        val pspec = PipelineSpec("test", modules = listOf("zvi-object-detection"))
        val pipeline = pipelineService.create(pspec)

        val rpipeline = pipelineResolverService.resolve(pipeline.name, listOf("zvi-label-detection"))
        Json.prettyPrint(rpipeline)
    }

    @Test
    fun resolveUsingPipelineNameAndMinusModules() {
        pipelineModService.updateStandardMods()

        val pspec = PipelineSpec("test", modules = listOf("zvi-extract-pages"))
        val pipeline = pipelineService.create(pspec)

        val rpipeline = pipelineResolverService.resolve(pipeline.name, listOf("-zvi-extract-pages"))
        val resolved = rpipeline.execute
        val last = resolved.last()

        assertEquals(last.className, "boonai_analysis.zvi.ZviSimilarityProcessor")
        assertEquals(last.image, "boonai/plugins-analysis")
    }

    @Test
    fun resolveNoOps() {
        val pipeline = pipelineService.create(PipelineSpec("test"))
        val resolved = pipelineResolverService.resolve(pipeline.id)
        // The resolved size is 1 less because the prepend marker is removed.
        assertEquals(resolved.execute.size, pipelineResolverService.getStandardPipeline().size - 1)
    }

    @Test
    fun testResolveDefaultPipeline() {
        val resolved = pipelineResolverService.resolve()
        // The resolved size is 1 less because the prepend marker is removed.
        assertEquals(resolved.execute.size, pipelineResolverService.getStandardPipeline().size - 1)
    }

    @Test
    @Ignore("Random failures in CICD only.")
    fun resolveLastOp() {
        val spec1 = PipelineModSpec(
            "append", "A append module",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(ProcessorRef("append_processor", "boonai-plugins-foo"))
                )
            )
        )
        val spec2 = PipelineModSpec(
            "last", "A last module",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(
                ModOp(
                    ModOpType.LAST,
                    listOf(ProcessorRef("last_processor", "boonai-plugins-foo"))
                )
            )
        )
        val mod1 = pipelineModService.create(spec1)
        val mod2 = pipelineModService.create(spec2)

        val pipeline = pipelineService.create(
            PipelineSpec(
                "test",
                modules = listOf(mod1.name, mod2.name)
            )
        )
        val rpipe = pipelineResolverService.resolve(pipeline.id)
        val resolved = rpipe.execute
        assertEquals("last_processor", resolved.last().className)
        assertEquals("append_processor", resolved[resolved.size - 2].className)
    }

    @Test
    fun resolveDependAddBefore() {
        val spec1 = PipelineModSpec(
            "append", "A append module",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(
                ModOp(ModOpType.DEPEND, listOf("depend-module")),
                ModOp(
                    ModOpType.APPEND,
                    listOf(ProcessorRef("append_processor", "boonai-plugins-foo"))
                )
            )
        )
        val spec2 = PipelineModSpec(
            "depend-module", "Added before",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(ProcessorRef("depend_processor", "boonai-plugins-foo"))
                )
            )
        )
        val mod1 = pipelineModService.create(spec1)
        pipelineModService.create(spec2)

        val pipeline = pipelineService.create(
            PipelineSpec(
                "test",
                modules = listOf(mod1.name)
            )
        )
        val rpipe = pipelineResolverService.resolve(pipeline.id)
        val resolved = rpipe.execute

        assertEquals("append_processor", resolved.last().className)
        assertEquals("depend_processor", resolved[resolved.size - 2].className)
    }

    @Test
    fun resolveAppendOp() {
        val spec = PipelineModSpec(
            "test", "A test module",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(ProcessorRef("foo", "boonai-plugins-foo"))
                )
            )
        )
        val resolved = setupTestPipeline(spec)
        assertEquals("foo", resolved.last().className)
    }

    @Test
    fun resolveAppendOnceOp() {
        val spec1 = PipelineModSpec(
            "test", "A test module",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(
                ModOp(
                    ModOpType.APPEND_MERGE,
                    listOf(ProcessorRef("Foo.FooProcessor", "boonai-plugins-foo", mapOf("dog" to "cat")))
                ),
                ModOp(
                    ModOpType.APPEND_MERGE,
                    listOf(ProcessorRef("Foo.FooProcessor", "boonai-plugins-foo", mapOf("capt" to "kirk")))
                )
            )
        )

        val spec2 = PipelineModSpec(
            "test2", "A test module",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(
                ModOp(
                    ModOpType.APPEND_MERGE,
                    listOf(ProcessorRef("Foo.FooProcessor", "boonai-plugins-foo", mapOf("capt" to "picard")))
                ),
                ModOp(
                    ModOpType.APPEND_MERGE,
                    listOf(ProcessorRef("Foo.FooProcessor", "boonai-plugins-foo", mapOf("bilbo" to "baggins")))
                )
            )
        )

        val resolved = setupTestPipeline(spec1, spec2)
        assertEquals(1, resolved.filter { it.className == "Foo.FooProcessor" }.size)

        val last = resolved.last()
        assertEquals("cat", last.args?.get("dog"))
        assertEquals("picard", last.args?.get("capt"))
        assertEquals("baggins", last.args?.get("bilbo"))
    }

    @Test
    fun resolvePrependOp() {
        val spec = PipelineModSpec(
            "test", "A test module",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(ProcessorRef("append", "boonai-plugins-foo"))
                ),
                ModOp(
                    ModOpType.PREPEND,
                    listOf(ProcessorRef("prepend", "boonai-plugins-foo"))
                )
            )
        )
        val resolved = setupTestPipeline(spec)
        assertEquals("prepend", resolved[resolved.size - 2].className)
    }

    @Test
    fun resolveRemoveOp() {
        val spec = PipelineModSpec(
            "test", "A test module",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(
                ModOp(
                    ModOpType.REMOVE,
                    null,
                    OpFilter(OpFilterType.SUBSTR, "FileImportProcessor")
                )
            )
        )
        val resolved = setupTestPipeline(spec)
        val idx = resolved.indexOfFirst { "FileImportProcessor" in it.className }
        assertEquals(-1, idx)
    }

    @Test
    fun resolveReplaceOp() {
        val spec = PipelineModSpec(
            "test", "A test module",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(
                ModOp(
                    ModOpType.REPLACE,
                    listOf(ProcessorRef("replaced", "boonai-plugins-foo")),
                    OpFilter(OpFilterType.SUBSTR, "FileImportProcessor")
                )
            )
        )
        val resolved = setupTestPipeline(spec)
        val idx = resolved.indexOfFirst { "replace" in it.className }
        assertEquals(1, idx)
    }

    @Test
    fun resolveAddBeforeOp() {
        val spec = PipelineModSpec(
            "test", "A test module",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(
                ModOp(
                    ModOpType.ADD_BEFORE,
                    listOf(ProcessorRef("replaced", "boonai-plugins-foo")),
                    OpFilter(OpFilterType.SUBSTR, "FileImportProcessor")
                )
            )
        )
        val resolved = setupTestPipeline(spec)
        val idx = resolved.indexOfFirst { "replace" in it.className }
        assertEquals(1, idx)
    }

    @Test
    fun resolveAddAfterOp() {
        val spec = PipelineModSpec(
            "test", "A test module",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(
                ModOp(
                    ModOpType.ADD_AFTER,
                    listOf(ProcessorRef("replaced", "boonai-plugins-foo")),
                    OpFilter(OpFilterType.SUBSTR, "FileImportProcessor")
                )
            )
        )
        val resolved = setupTestPipeline(spec)
        val idx = resolved.indexOfFirst { "replace" in it.className }
        assertEquals(2, idx)
    }

    @Test
    fun resolveSetArgsOp() {
        val spec = PipelineModSpec(
            "test", "A test module",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(
                ModOp(
                    ModOpType.SET_ARGS,
                    mapOf("extract_doc_pages" to true),
                    OpFilter(OpFilterType.SUBSTR, "FileImportProcessor")
                )
            )
        )
        val resolved = setupTestPipeline(spec)
        val idx = resolved.indexOfFirst { it.args?.get("extract_doc_pages") == true }
        assertEquals(1, idx)
    }

    @Test
    fun resolveRegexMatcher() {
        val spec = PipelineModSpec(
            "test", "A test module",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(
                ModOp(
                    ModOpType.REMOVE,
                    null,
                    OpFilter(OpFilterType.REGEX, ".*ImageProxyProcessor|.*VideoProxyProcessor"),
                    maxApplyCount = 2
                )
            )
        )
        val resolved = setupTestPipeline(spec)
        assertEquals(-1, resolved.indexOfFirst { "ImageProxyProcessor" in it.className })
        assertEquals(-1, resolved.indexOfFirst { "VideoProxyProcessor" in it.className })
    }

    @Test
    fun testResolveMaxApplyCount() {
        val spec = PipelineModSpec(
            "test", "A test module",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(
                ModOp(
                    ModOpType.REMOVE,
                    null,
                    OpFilter(OpFilterType.REGEX, ".*ImageProxyProcessor|.*VideoProxyProcessor"),
                    maxApplyCount = 1
                )
            )
        )
        // Op only applied 1 time.
        val resolved = setupTestPipeline(spec)
        assertEquals(-1, resolved.indexOfFirst { "ImageProxyProcessor" in it.className })
        assertEquals(2, resolved.indexOfFirst { "VideoProxyProcessor" in it.className })
    }

    /**
     * Makes a test pipeline with the given module and resolves
     * into a list of [ProcessorRef]
     */
    private fun setupTestPipeline(spec: PipelineModSpec, spec2: PipelineModSpec? = null): List<ProcessorRef> {
        val mods = mutableListOf<UUID>()
        mods.add(pipelineModService.create(spec).id)
        if (spec2 != null) {
            mods.add(pipelineModService.create(spec2).id)
        }
        entityManager.flush()
        val pipeline = pipelineService.create(PipelineSpec("test"))
        pipelineService.update(
            pipeline.id,
            PipelineUpdate(
                pipeline.name, pipeline.processors, mods
            )
        )
        return pipelineResolverService.resolve(pipeline.id).execute
    }
}
