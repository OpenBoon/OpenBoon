package boonai.archivist.storage

import boonai.archivist.AbstractTest
import boonai.archivist.domain.FileStorage
import boonai.archivist.domain.ProjectFileLocator
import boonai.archivist.domain.ProjectStorageCategory
import boonai.archivist.domain.ProjectStorageEntity
import boonai.archivist.domain.ProjectStorageSpec
import boonai.archivist.domain.Task
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.logging.Logging
import com.google.cloud.logging.LoggingOptions
import com.google.cloud.logging.Payload
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Ignore
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.springframework.core.io.InputStreamResource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import java.io.BufferedReader
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GcsProjectStorageServiceTests : AbstractTest() {

    @Mock
    lateinit var googleStorageService: GcsProjectStorageService

    @Test
    fun storeTest() {
        whenever(googleStorageService.store(any())).thenReturn(
            FileStorage(
                "1234",
                "test_path",
                "source",
                "image/jpeg",
                4,
                mapOf("cats" to 100)
            )
        )

        val loc = ProjectFileLocator(
            ProjectStorageEntity.ASSETS,
            "1234", ProjectStorageCategory.SOURCE, "bob.jpg", projectId = UUID.randomUUID()
        )
        val spec = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())

        val result = googleStorageService.store(spec)
        assertEquals("1234", result.id)
        assertEquals("source", result.category)
        assertEquals("test_path", result.name)
        assertEquals(4, result.size)
        assertEquals("image/jpeg", result.mimetype)
        assertEquals(mapOf("cats" to 100), result.attrs)
    }

    @Test
    fun recursiveDeleteTest() {
        whenever(googleStorageService.recursiveDelete(anyString())).then {}
        whenever(googleStorageService.listFiles(anyString())).thenReturn(emptyList())

        googleStorageService.recursiveDelete("bucket-name/project-id")

        val listOfItens = googleStorageService.listFiles("bucket-name/project-id")

        assert(listOfItens.isEmpty())
    }

    @Test
    fun streamLogsTest() {

        val allLogs = "Success"
        val response = ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .contentLength(allLogs.length.toLong())
            .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
            .body(InputStreamResource(allLogs.byteInputStream()))
        doReturn(response).`when`(googleStorageService).streamLogs(any())

        val loc = ProjectFileLocator(
            ProjectStorageEntity.ASSETS,
            "1234", ProjectStorageCategory.SOURCE, "bob.jpg", projectId = UUID.randomUUID()
        )

        val task = mock(Task::class.java)
        whenever(task.projectId).thenReturn(UUID.randomUUID())
        whenever(task.id).thenReturn(UUID.randomUUID())
        whenever(task.runCount).thenReturn(1)

        val res = googleStorageService.streamLogs(task)

        assertEquals(200, res.statusCode.value())
        assertEquals(allLogs, BufferedReader(res.body.inputStream.reader()).readLine())
    }

    @Test
    @Ignore
    fun streamLogsFunctionalTest() {
        // Log something with custom names before executing
        val fromStream: GoogleCredentials? = null

        val newBuilder = LoggingOptions.newBuilder()
        fromStream?.let { newBuilder.setCredentials(fromStream) }
        val build = newBuilder.build()
        val logging = build.service

        val logger_name = "testing_custom_logs1"
        var logsPagedResponse = logging.listLogEntries(Logging.EntryListOption.filter(logger_name))

        val allLogs = logsPagedResponse.iterateAll().joinToString(separator = "\n") {
            it.getPayload<Payload.StringPayload>().data
        }

        assertNotEquals(0, allLogs.length)
    }
}
