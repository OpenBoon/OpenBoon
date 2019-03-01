package com.zorroa.archivist.rest

import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.archivist.domain.FieldEdit
import com.zorroa.archivist.domain.FieldEditSpec
import com.zorroa.archivist.domain.FieldSpec
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.repository.IndexDao
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.Json
import org.junit.Assert
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.*
import kotlin.test.assertEquals

class FieldEditControllerTests : MockMvcTest() {

    @Autowired
    lateinit var indexDao: IndexDao

    @Test
    fun testGetFieldEdit() {
        val session = admin()
        addTestAssets("set04/standard")
        var asset = indexDao.getAll(Pager.first())[0]

        val field = fieldSystemService.createField(FieldSpec("File Ext",
                "source.extension", null, true))
        val edit = assetService.createFieldEdit(FieldEditSpec(UUID.fromString(asset.id), field.id,
                "source.extension", "gandalf"))

        val req = mvc.perform(MockMvcRequestBuilders.get("/api/v1/fieldEdits/${edit.id}")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val result = Json.Mapper.readValue<FieldEdit>(req.response.contentAsString)
        assertEquals(edit.id, result.id)
    }


    @Test
    fun testSearchFieldEdits() {
        val session = admin()
        addTestAssets("set04/standard")
        var asset = indexDao.getAll(Pager.first())[0]

        val field = fieldSystemService.createField(FieldSpec("File Ext",
                "source.extension", null, true))
        assetService.createFieldEdit(FieldEditSpec(UUID.fromString(asset.id), field.id,
                "source.extension", "gandalf"))

        val req = mvc.perform(MockMvcRequestBuilders.post("/api/v1/fieldEdits/_search")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val result = Json.Mapper.readValue<KPagedList<FieldEdit>>(
                req.response.contentAsString, FieldEdit.Companion.TypeRefKList)
        Assert.assertEquals(1, result.size())
    }

    @Test
    fun testCreateFieldEdit() {
        val session = admin()
        addTestAssets("set04/standard")
        var asset = indexDao.getAll(Pager.first())[0]

        val field = fieldSystemService.createField(FieldSpec("File Ext",
                "source.extension", null, true))
        val spec = FieldEditSpec(UUID.fromString(asset.id), field.id, null, newValue="bob")

        val req = mvc.perform(MockMvcRequestBuilders.post("/api/v1/fieldEdits")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serializeToString(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val result = Json.Mapper.readValue<FieldEdit>(req.response.contentAsString, FieldEdit::class.java)
        Assert.assertEquals(field.id, result.fieldId)
        Assert.assertEquals(UUID.fromString(asset.id), result.assetId)
        Assert.assertEquals(spec.newValue, result.newValue)
        Assert.assertEquals("jpg", result.oldValue)
    }

    @Test
    fun testDeleteFieldEdit() {

        val session = admin()
        addTestAssets("set04/standard")
        var asset = indexDao.getAll(Pager.first())[0]

        val field = fieldSystemService.createField(FieldSpec("File Ext",
                "source.extension", null, true))
        val edit = assetService.createFieldEdit(FieldEditSpec(asset.id, field.id,
                "source.extension", "gandalf"))

        val req = mvc.perform(MockMvcRequestBuilders.delete(
                "/api/v1/fieldEdits/${edit.id}")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serializeToString(edit))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val result = Json.Mapper.readValue<Map<String,Any>>(req.response.contentAsString, Json.GENERIC_MAP)
        Assert.assertEquals("delete", result["op"])
        Assert.assertEquals(true, result["success"])
    }
}