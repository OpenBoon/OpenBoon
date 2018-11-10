package com.zorroa.archivist.web

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.collect.ImmutableList
import com.zorroa.archivist.domain.*
import com.zorroa.common.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MvcResult

import java.io.File
import java.text.ParseException

import org.junit.Assert.assertEquals
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Created by chambers on 4/18/17.
 */
class DyhierarchyControllerTests : MockMvcTest() {


    @Before
    @Throws(ParseException::class)
    fun init() {
        for (f in getTestImagePath("set01").toFile().listFiles()!!) {
            if (!f.isFile || f.isHidden) {
                continue
            }
            val ab = Source(f)
            ab.setAttr("tree.path", ImmutableList.of("/foo/bar/", "/bing/bang/", "/foo/shoe/"))
            indexService.index(ab)
        }
        refreshIndex()
    }

    @Test
    @Throws(Exception::class)
    fun testCreate() {
        val session = admin()

        val (id) = folderService.create(FolderSpec("foo"), false)
        val spec = DyHierarchySpec()
        spec.folderId = id
        spec.levels = ImmutableList.of(
                DyHierarchyLevel("source.date", DyHierarchyLevelType.Day),
                DyHierarchyLevel("source.type.raw"),
                DyHierarchyLevel("source.extension.raw"),
                DyHierarchyLevel("source.filename.raw"))

        val result = mvc.perform(post("/api/v1/dyhi")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(status().isOk)
                .andReturn()

        val dh = Json.Mapper.readValue<DyHierarchy>(result.response.contentAsString,
                object : TypeReference<DyHierarchy>() {

                })
        assertEquals(4, dh.levels.size.toLong())
    }
}
