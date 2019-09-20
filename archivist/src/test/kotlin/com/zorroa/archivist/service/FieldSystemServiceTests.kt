package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AttrType
import com.zorroa.archivist.domain.FieldEditSpec
import com.zorroa.archivist.domain.FieldSpecCustom
import com.zorroa.archivist.domain.FieldSpecExpose
import com.zorroa.archivist.domain.FieldUpdateSpec
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.common.domain.JobFilter
import com.zorroa.common.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FieldSystemServiceTests : AbstractTest() {

    @Autowired
    lateinit var jobService: JobService

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Before
    fun init() {
        addTestAssets("set04/standard")
        refreshIndex()
    }

    @Test
    fun createRegular() {
        val spec = FieldSpecExpose("File Extension", "source.extension")
        val field = fieldSystemService.createField(spec)
        assertEquals("source.extension", field.attrName)
        assertEquals("File Extension", field.name)
        assertEquals(field.attrType, AttrType.StringAnalyzed)
        assertEquals(false, field.editable)
    }

    @Test
    fun createCustomStringContentField() {
        val fieldName = "test.test_content"
        val spec = FieldSpecCustom("Notes", fieldName, AttrType.StringContent).apply { editable = true }
        val field = fieldSystemService.createField(spec)
        assertEquals(AttrType.StringContent, field.attrType)
        assertTrue(field.custom)
        assertEquals(fieldName, field.attrName)
        assertEquals("Notes", field.name)

        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, "ABC"))
        assertEquals(AttrType.StringContent, fieldSystemService.getEsAttrType(fieldName))
    }

    @Test
    fun createCustomStringExactField() {
        val fieldName = "test.str_exact"
        val spec = FieldSpecCustom("SomeField", fieldName, AttrType.StringExact).apply { editable = true }
        val field = fieldSystemService.createField(spec)
        assertEquals(AttrType.StringExact, field.attrType)
        assertTrue(field.custom)
        assertEquals(fieldName, field.attrName)

        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, "ABC"))
        assertEquals(AttrType.StringExact, fieldSystemService.getEsAttrType(fieldName))
    }

    @Test
    fun createCustomStringAnalyzedField() {
        val attrName = "test.str_analyzed"
        val attrType = AttrType.StringAnalyzed

        val spec = FieldSpecCustom("SomeField", attrName, attrType).apply { editable = true }
        val field = fieldSystemService.createField(spec)
        assertEquals(attrType, field.attrType)
        assertTrue(field.custom)
        assertEquals(attrName, field.attrName)

        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, "ABC"))
        assertEquals(attrType, fieldSystemService.getEsAttrType(attrName))
    }

    @Test
    fun createCustomStringAnalyzedArrayField() {
        val attrName = "test.str_analyzed"
        val attrType = AttrType.StringAnalyzed

        val spec = FieldSpecCustom("SomeField", attrName, attrType).apply { editable = true }
        val field = fieldSystemService.createField(spec)
        assertEquals(attrType, field.attrType)
        assertTrue(field.custom)
        assertEquals(attrName, field.attrName)

        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, listOf("ABC")))
        assertEquals(attrType, fieldSystemService.getEsAttrType(attrName))
    }

    @Test
    fun createCustomStringPathField() {
        val attrName = "test.path_test"
        val attrType = AttrType.StringPath

        val spec = FieldSpecCustom("SomeField", attrName, attrType).apply { editable = true }
        val field = fieldSystemService.createField(spec)
        assertEquals(attrType, field.attrType)
        assertTrue(field.custom)
        assertEquals(attrName, field.attrName)

        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, "/ABC/123"))
        assertEquals(attrType, fieldSystemService.getEsAttrType(attrName))
    }

    @Test
    fun createCustomNumberIntegerField() {
        val attrName = "test.number_test"
        val attrType = AttrType.NumberInteger

        val spec = FieldSpecCustom("SomeField", attrName, attrType).apply { editable = true }
        val field = fieldSystemService.createField(spec)
        assertEquals(attrType, field.attrType)
        assertTrue(field.custom)
        assertEquals(attrName, field.attrName)

        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, 2112))
        assertEquals(attrType, fieldSystemService.getEsAttrType(attrName))
    }

    @Test
    fun createCustomNumberIntegerArrayField() {
        val attrName = "test.number_test"
        val attrType = AttrType.NumberInteger

        val spec = FieldSpecCustom("SomeField", attrName, attrType).apply { editable = true }
        val field = fieldSystemService.createField(spec)
        assertEquals(attrType, field.attrType)
        assertTrue(field.custom)
        assertEquals(attrName, field.attrName)

        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, listOf(2112)))
        assertEquals(attrType, fieldSystemService.getEsAttrType(attrName))
    }

    @Test
    fun createCustomNumberFloatField() {
        val attrName = "test.float_test"
        val attrType = AttrType.NumberFloat

        val spec = FieldSpecCustom("SomeField", attrName, attrType).apply { editable = true }
        val field = fieldSystemService.createField(spec)
        assertEquals(attrType, field.attrType)
        assertTrue(field.custom)
        assertEquals(attrName, field.attrName)

        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, 2.22f))
        assertEquals(attrType, fieldSystemService.getEsAttrType(attrName))
    }

    @Test
    fun createCustomNumberFloatArrayField() {
        val attrName = "test.float_test"
        val attrType = AttrType.NumberFloat

        val spec = FieldSpecCustom("SomeField", attrName, attrType).apply { editable = true }
        val field = fieldSystemService.createField(spec)
        assertEquals(attrType, field.attrType)
        assertTrue(field.custom)
        assertEquals(attrName, field.attrName)

        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, listOf(2.22f)))
        assertEquals(attrType, fieldSystemService.getEsAttrType(attrName))
    }

    @Test
    fun createCustomBooleanField() {
        val attrName = "test.bool_test"
        val attrType = AttrType.Bool

        val spec = FieldSpecCustom("SomeField", attrName, attrType).apply { editable = true }
        val field = fieldSystemService.createField(spec)
        assertEquals(attrType, field.attrType)
        assertTrue(field.custom)
        assertEquals(attrName, field.attrName)

        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, true))
        assertEquals(attrType, fieldSystemService.getEsAttrType(attrName))
    }

    @Test
    fun createCustomField() {
        val attrName = "test.bool_test"
        val attrType = AttrType.Bool

        val spec = FieldSpecCustom("SomeField", attrName, attrType).apply { editable = true }
        val field = fieldSystemService.createField(spec)
        assertEquals(attrType, field.attrType)
        assertTrue(field.custom)
        assertEquals(attrName, field.attrName)

        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, true))
        assertEquals(attrType, fieldSystemService.getEsAttrType(attrName))
    }

    @Test
    fun createCustomDateTimeFieldAsLong() {
        val attrName = "test.date_test"
        val attrType = AttrType.DateTime

        val spec = FieldSpecCustom("SomeField", attrName, attrType).apply { editable = true }
        val field = fieldSystemService.createField(spec)
        assertEquals(attrType, field.attrType)
        assertTrue(field.custom)
        assertEquals(attrName, field.attrName)

        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, System.currentTimeMillis()))
        assertEquals(attrType, fieldSystemService.getEsAttrType(attrName))
    }

    @Test
    fun createCustomDateTimeFieldAsString() {
        val attrName = "test.date_test"
        val attrType = AttrType.DateTime

        val spec = FieldSpecCustom("SomeField", attrName, attrType).apply { editable = true }
        val field = fieldSystemService.createField(spec)
        assertEquals(attrType, field.attrType)
        assertTrue(field.custom)
        assertEquals(attrName, field.attrName)

        val date = "11/12/1974 10:14:52"
        println(date)
        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, date))
        assertEquals(attrType, fieldSystemService.getEsAttrType(attrName))
    }

    @Test
    fun applyFieldEdits() {
        setupEmbeddedFieldSets()
        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        val field = fieldSystemService.getField("media.title")
        val edit = assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, "bilbo"))

        fieldSystemService.applyFieldEdits(asset)
        assertEquals("bilbo", asset.getAttr("media.title", String::class.java))
    }

    @Test
    fun getEsTypeMap() {
        val typeMap = fieldSystemService.getEsTypeMap()
        assertEquals(AttrType.StringAnalyzed, typeMap["source.filename"])
        assertEquals(AttrType.StringExact, typeMap["media.clip.parent"])
        assertEquals(AttrType.NumberFloat, typeMap["media.clip.length"])
        assertEquals(AttrType.NumberInteger, typeMap["media.width"])
        assertEquals(AttrType.GeoPoint, typeMap["location.point"])
        assertEquals(AttrType.DateTime, typeMap["system.timeModified"])
    }

    @Test
    fun getEsAttrType() {
        assertEquals(AttrType.StringAnalyzed, fieldSystemService.getEsAttrType("source.filename"))
        assertEquals(AttrType.StringExact, fieldSystemService.getEsAttrType("media.clip.parent"))
        assertEquals(AttrType.NumberFloat, fieldSystemService.getEsAttrType("media.clip.length"))
        assertEquals(AttrType.NumberInteger, fieldSystemService.getEsAttrType("media.width"))
        assertEquals(AttrType.GeoPoint, fieldSystemService.getEsAttrType("location.point"))
        assertEquals(AttrType.DateTime, fieldSystemService.getEsAttrType("system.timeModified"))
    }

    @Test
    fun getEsMapping() {
        // Not testing this 2 hard, the getEsAttrType and  getEsTypeMap are testing it more in depth.
        val mapping = fieldSystemService.getEsMapping()
        assertTrue("unittest" in mapping)
    }

    @Test
    fun testCreateSuggestField() {
        val jobCount = jobService.getAll(JobFilter()).size()
        val spec = FieldSpecExpose("File Extension", "foo.bar", AttrType.StringAnalyzed).apply {
            suggest = true
            forceType = true
        }
        val field = fieldSystemService.createField(spec)
        assertEquals(true, field.suggest)
        assertEquals(true, field.keywords)
        assertEquals(
            jobCount + 1, jobService.getAll(JobFilter()).size(),
            "reindex job was not created"
        )
    }

    @Test
    fun testCreateSuggestFieldSkipReindex() {
        val jobCount = jobService.getAll(JobFilter()).size()
        val spec = FieldSpecExpose("File Extension", "foo.bar", AttrType.StringAnalyzed).apply {
            suggest = true
            forceType = true
        }
        fieldSystemService.createField(spec, reindexSuggest = false)
        assertEquals(
            jobCount, jobService.getAll(JobFilter()).size(),
            "reindex job was created but was not needed"
        )
    }

    @Test
    fun testUpdateFieldSuggest() {
        val jobCount = jobService.getAll(JobFilter()).size()
        val spec = FieldSpecExpose("File Extension", "foo.bar", AttrType.StringAnalyzed).apply {
            forceType = true
        }

        val field = fieldSystemService.createField(spec, true)
        assertEquals(
            jobCount, jobService.getAll(JobFilter()).size(),
            "reindex job was created but was not needed"
        )

        assertTrue(
            fieldSystemService.updateField(
                field, FieldUpdateSpec(
                    field.name, field.editable,
                    field.keywords, field.keywordsBoost, true, requireList = false
                )
            )
        )
        assertEquals(
            jobCount + 1, jobService.getAll(JobFilter()).size(),
            "reindex job was not created"
        )
    }

    @Test
    fun testApplySuggestFields() {
        val spec = FieldSpecExpose("File Extension", "source.extension", AttrType.StringAnalyzed).apply {
            suggest = true
        }
        fieldSystemService.createField(spec, reindexSuggest = false)
        val assets = searchService.search(Pager.first(), AssetSearch()).list
        fieldSystemService.applySuggestions(assets)
        for (asset in assets) {
            assertTrue(
                asset.getAttr("system.suggestions", Json.LIST_OF_STRINGS).contains(
                    asset.getAttr("source.extension", String::class.java)
                )
            )
        }
    }
}
