package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.FolderSpec
import com.zorroa.archivist.domain.Taxonomy
import com.zorroa.archivist.domain.TaxonomySpec
import com.zorroa.archivist.service.TaxonomyService
import com.zorroa.common.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Created by chambers on 6/22/17.
 */
class TaxonomyControllerTests : MockMvcTest() {

    @Autowired
    internal var taxonomyService: TaxonomyService? = null

    @Test
    @Throws(Exception::class)
    fun testCreate() {
        val session = admin()

        val f = folderService.create(FolderSpec("bob"))
        val spec = TaxonomySpec(f)

        val result = mvc.perform(post("/api/v1/taxonomy")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val t = deserialize(result, Taxonomy::class.java)
        assertEquals(f.id, t.folderId)
    }

    @Test
    @Throws(Exception::class)
    fun testDelete() {
        val session = admin()

        val f = folderService.create(FolderSpec("bob"))
        val spec = TaxonomySpec(f)
        val tax = taxonomyService!!.create(spec)

        val result = mvc.perform(delete("/api/v1/taxonomy/" + tax.taxonomyId)
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val m = deserialize(result, Map::class.java)
        assertTrue(m["success"] as Boolean)
    }

}
