package com.zorroa

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals
import org.junit.Test

class StorageManagerTests {

    @Test
    fun testCleanup() {
        ServerOptions.storagePath = "/tmp/storage-cleanup-test"

        val sm = StorageManager
        Files.createDirectories(Paths.get(ServerOptions.storagePath).resolve("20191016"))
        assertEquals(1, sm.cleanup())
    }
}
