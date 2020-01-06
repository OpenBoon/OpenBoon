package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.OpFilter
import com.zorroa.archivist.domain.OpFilterType
import com.zorroa.archivist.domain.ModOp
import com.zorroa.archivist.domain.ModOpType
import com.zorroa.archivist.domain.PipelineModSpec
import com.zorroa.archivist.domain.PipelineSpec
import com.zorroa.archivist.domain.PipelineUpdate
import com.zorroa.archivist.domain.ProcessorRef
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
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
    fun resolveNoOps() {
        val pipeline = pipelineService.create(PipelineSpec("test"))
        val resolved = pipelineResolverService.resolve(pipeline.id)
        assertEquals(STANDARD_PIPELINE.size, resolved.size)
    }

    @Test
    fun resolveAppendOp() {
        val spec = PipelineModSpec(
            "test", "A test module",
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(ProcessorRef("foo", "zmlp-plugins-foo"))
                )
            )
        )
        val resolved = setupTestPipeline(spec)
        assertEquals("foo", resolved.last().className)
    }

    @Test
    fun resolvePrependOp() {
        val spec = PipelineModSpec(
            "test", "A test module",
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(ProcessorRef("append", "zmlp-plugins-foo"))
                ),
                ModOp(
                    ModOpType.PREPEND,
                    listOf(ProcessorRef("prepend", "zmlp-plugins-foo"))
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
            listOf(
                ModOp(
                    ModOpType.REMOVE,
                    null,
                    OpFilter(OpFilterType.SUBSTR, "OfficeImporter")
                )
            )
        )
        val resolved = setupTestPipeline(spec)
        val idx = resolved.indexOfFirst { "OfficeImporter" in it.className }
        assertEquals(-1, idx)
    }

    @Test
    fun resolveReplaceOp() {
        val spec = PipelineModSpec(
            "test", "A test module",
            listOf(
                ModOp(
                    ModOpType.REPLACE,
                    listOf(ProcessorRef("replaced", "zmlp-plugins-foo")),
                    OpFilter(OpFilterType.SUBSTR, "OfficeImporter")
                )
            )
        )
        val resolved = setupTestPipeline(spec)
        val idx = resolved.indexOfFirst { "replace" in it.className }
        assertEquals(2, idx)
    }

    @Test
    fun resolveAddBeforeOp() {
        val spec = PipelineModSpec(
            "test", "A test module",
            listOf(
                ModOp(
                    ModOpType.ADD_BEFORE,
                    listOf(ProcessorRef("replaced", "zmlp-plugins-foo")),
                    OpFilter(OpFilterType.SUBSTR, "OfficeImporter")
                )
            )
        )
        val resolved = setupTestPipeline(spec)
        val idx = resolved.indexOfFirst { "replace" in it.className }
        assertEquals(2, idx)
    }

    @Test
    fun resolveAddAfterOp() {
        val spec = PipelineModSpec(
            "test", "A test module",
            listOf(
                ModOp(
                    ModOpType.ADD_AFTER,
                    listOf(ProcessorRef("replaced", "zmlp-plugins-foo")),
                    OpFilter(OpFilterType.SUBSTR, "OfficeImporter")
                )
            )
        )
        val resolved = setupTestPipeline(spec)
        val idx = resolved.indexOfFirst { "replace" in it.className }
        assertEquals(3, idx)
    }

    @Test
    fun resolveSetArgsOp() {
        val spec = PipelineModSpec(
            "test", "A test module",
            listOf(
                ModOp(
                    ModOpType.SET_ARGS,
                    mapOf("extract_pages" to true),
                    OpFilter(OpFilterType.SUBSTR, "OfficeImporter")
                )
            )
        )
        val resolved = setupTestPipeline(spec)
        val idx = resolved.indexOfFirst { it.args?.get("extract_pages") == true }
        assertEquals(2, idx)
    }

    @Test
    fun resolveRegexMatcher() {
        val spec = PipelineModSpec(
            "test", "A test module",
            listOf(
                ModOp(
                    ModOpType.REMOVE,
                    null,
                    OpFilter(OpFilterType.REGEX, ".*OfficeImporter|.*VideoImporter"),
                    maxApplyCount = 2
                )
            )
        )
        val resolved = setupTestPipeline(spec)
        assertEquals(-1, resolved.indexOfFirst { "VideoImporter" in it.className })
        assertEquals(-1, resolved.indexOfFirst { "OfficeImporter" in it.className })
    }

    @Test
    fun testResolveMaxApplyCount() {
        val spec = PipelineModSpec(
            "test", "A test module",
            listOf(
                ModOp(
                    ModOpType.REMOVE,
                    null,
                    OpFilter(OpFilterType.REGEX, ".*OfficeImporter|.*VideoImporter"),
                    maxApplyCount = 1
                )
            )
        )
        // Op only applied 1 time.
        val resolved = setupTestPipeline(spec)
        assertEquals(-1, resolved.indexOfFirst { "OfficeImporter" in it.className })
        assertEquals(2, resolved.indexOfFirst { "VideoImporter" in it.className })
    }

    /**
     * Makes a test pipeline with the given module and resolves
     * into a list of [ProcessorRef]
     */
    private fun setupTestPipeline(spec: PipelineModSpec): List<ProcessorRef> {
        val mod = pipelineModService.create(spec)
        entityManager.flush()
        val pipeline = pipelineService.create(PipelineSpec("test"))
        pipelineService.update(
            pipeline.id, PipelineUpdate(
                pipeline.name, pipeline.processors, listOf(mod.id)
            )
        )
        return pipelineResolverService.resolve(pipeline.id)
    }
}