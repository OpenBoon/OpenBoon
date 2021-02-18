package boonai.similarity

import org.junit.Test
import kotlin.test.assertEquals

class SimilarityEngineTests {

    @Test
    fun testSingleHash() {
        val hashes = listOf("AAAAAAAA")
        val engine = SimilarityEngine(
            hashes,
            0.75,
            16
        )

        assertEquals(0.9375, engine.execute(listOf("BBBBBBBB")))
    }

    @Test
    fun testMultipleQueryHash() {
        val hashes = listOf("AAAAAAAA", "BBBBBBBB")
        val engine = SimilarityEngine(
            hashes,
            0.75,
            16
        )

        assertEquals(1.0, engine.execute(listOf("BBBBBBBB")))
    }

    @Test
    fun testMultipleAssetAndQueryHash() {
        val hashes = listOf("AAAAAAAA", "CCCCCCCC")
        val engine = SimilarityEngine(
            hashes,
            0.75,
            16
        )

        assertEquals(0.875, engine.execute(listOf("EEEEEEEE", "GGGGGGGG")))
    }
}
