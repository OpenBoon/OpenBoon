package com.zorroa

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestGcsStorage {
    var client = mock(GcsStorageClient::class.java)

    @Test
    fun testExistBucket() {
        whenever(client.bucketExists(anyString())).thenReturn(true)

        assertTrue(client.bucketExists("test-bucket"))
    }

    @Test
    fun testFetchObject() {
        whenever(client.fetch(anyString())).thenReturn("ReturnValue".byteInputStream())

        val fetch = client.fetch("cat.0.jpg")
        assertEquals(true, fetch.available() > 0)
    }

    @Test
    fun testStoreObject() {
        val content = "testText"
        val fileType = "text"
        val path = "test.txt"

        doNothing().`when`(client).store(anyString(), any(), anyLong(), anyString())
        doNothing().`when`(client).delete(anyString())

        whenever(client.fetch(anyString()))
            .thenReturn(content.byteInputStream())
            .thenThrow(NullPointerException::class.java)

        client.store(path, content.byteInputStream(), content.length.toLong(), fileType)

        val fetch = client.fetch(path)
        assertEquals(content.length, fetch.available())

        client.delete(path)

        assertFailsWith<NullPointerException> { client.fetch(path) }
    }
}
