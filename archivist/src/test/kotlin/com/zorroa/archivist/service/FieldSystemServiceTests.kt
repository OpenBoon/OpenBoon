package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AttrType
import com.zorroa.archivist.domain.FieldEditSpec
import com.zorroa.archivist.domain.FieldSpec
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.search.AssetSearch
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FieldSystemServiceTests : AbstractTest() {

    @Before
    fun init() {
        addTestAssets("set04/standard")
        refreshIndex()
    }

    @Test
    fun createRegular() {
        val spec = FieldSpec("File Extension", "source.extension", null,false)
        val field = fieldSystemService.createField(spec)
        assertEquals("source.extension", field.attrName)
        assertEquals("File Extension", field.name)
        assertEquals(field.attrType, AttrType.String)
        assertEquals(false, field.editable)
    }

    @Test
    fun createCustom() {
        val spec = FieldSpec("Notes", null, AttrType.StringContent,false)
        val field = fieldSystemService.createField(spec)
        assertEquals(AttrType.StringContent, field.attrType)
        assertTrue(field.custom)
        assertEquals("custom.stringcontent__0", field.attrName)
        assertEquals("Notes", field.name)
    }

    @Test
    fun applyFieldEdits() {
        val asset =  searchService.search(Pager.first(), AssetSearch()).list.first()
        val field = fieldSystemService.getField("media.title")
        val edit = assetService.edit(asset.id, FieldEditSpec(field.id, null, "bilbo"))

        fieldSystemService.applyFieldEdits(asset)
        assertEquals("bilbo", asset.getAttr("media.title", String::class.java))
    }
}