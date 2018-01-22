package com.zorroa.cluster.zps

import com.zorroa.sdk.domain.Document
import com.zorroa.sdk.processor.SharedData
import com.zorroa.sdk.util.Json
import org.junit.Ignore
import org.junit.Test
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.atomic.LongAdder
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Created by chambers on 3/23/17.
 */
class MetaZpsExecutorTests {

    /**
     * This test requires python core to be installed.
     */
    @Ignore
    @Test
    fun hybridTestReponseReaction() {
        val zpsTask = ZpsTask(
                "../../zorroa-test-data/scripts/hybrid/reaction_rsp.zps")
        val meta = MetaZpsExecutor(zpsTask, SharedData("../unittest/shared"))
        assertEquals(0, meta.execute())
        meta.addReactionHandler { task, shared, reaction ->
            val doc = Json.Mapper.convertValue(reaction.response, Document::class.java)
            assertEquals("bar", doc.getAttr<String>("foo"))
            assertEquals("bang", doc.getAttr<String>("bing"))
        }
    }

    @Test
    @Throws(IOException::class)
    fun testDetectSitePackages() {
        val rootPath = Files.createTempDirectory("zorroa-sdk")
        val sitePath = rootPath.resolve("plugins/sdk-test/site-packages")
        sitePath.toFile().mkdirs()

        val zpsTask = ZpsTask(
                "../../zorroa-test-data/scripts/hybrid/reaction_rsp.zps")

        val meta = MetaZpsExecutor(zpsTask, SharedData(rootPath))
        val pb = meta.buildProcess(arrayOf())

        assertTrue(pb.environment()["PYTHONPATH"]!!.contains("plugins/sdk-test/site-packages"))
    }

    /**
     * This test has a chicken before the egg problem, you need lang-python to test.
     */
    @Ignore
    @Test
    fun testResponseReactionPython() {
        val counter = LongAdder()
        val zpsTask = ZpsTask(
                "../../zorroa-test-data/scripts/python/reaction_rsp.zps")
        val meta = MetaZpsExecutor(zpsTask, SharedData("../unittest/shared"))
        meta.addReactionHandler { task, shared, reaction ->
            val doc = Json.Mapper.convertValue(reaction.response, Document::class.java)
            assertEquals("bar", doc.getAttr<String>("foo"))
            counter.increment()
        }
        meta.execute()
        assertEquals(1, counter.toInt())
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MetaZpsExecutorTests::class.java)
    }
}
