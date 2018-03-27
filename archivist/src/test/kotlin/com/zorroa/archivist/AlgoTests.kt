package com.zorroa.archivist

import com.zorroa.sdk.util.Json
import org.junit.Test
import java.util.*
import kotlin.test.assertTrue

class AlgoTests {

    @Test
    fun testInt() {
        val foo = 1
        println(foo.toString())
    }

    @Test
    fun testUUIDInSet() {

        val id1 = UUID.fromString("00000000-0000-0000-0000-000000000000")
        val id2 = UUID.fromString("00000000-0000-0000-0000-000000000000")

        val set = mutableSetOf<UUID>()
        set.add(id1)
        assertTrue(set.contains(id1))
        assertTrue(set.contains(id2))

    }

    @Test
    fun testSplitToMap() {
        val fields = "foo:1,bar:2"

        val staticKwMap = fields.splitToSequence(",")
                .map { it.split(":", limit = 2) }
                .map {it[0] to it[1] }.toMap()

        print(Json.serializeToString(staticKwMap))

    }
}
