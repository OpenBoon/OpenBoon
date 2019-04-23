package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.FieldSetSpec
import com.zorroa.archivist.domain.FieldSpec
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class FieldSetDaoTests : AbstractTest() {

    @Autowired
    lateinit var fieldSetDao: FieldSetDao

    override fun requiresElasticSearch() : Boolean {
        return true
    }

    @Test
    fun testCreate() {
        val spec = FieldSetSpec("The Shire")
        val set = fieldSetDao.create(spec)

        var count = jdbc.queryForObject("SELECT COUNT(1) FROM field_set_member WHERE pk_field_set=?",
                Int::class.java, set.id)
        assertEquals(0, count)
    }

    @Test
    fun testCreateWithFields() {

        val fspec = FieldSpec("Time Created", "system.timeCreated", null, false)
        val field = fieldSystemService.createField(fspec)

        val spec = FieldSetSpec("System", fieldIds = listOf(field.id))
        val set = fieldSetDao.create(spec)

        var count = jdbc.queryForObject("SELECT COUNT(1) FROM field_set_member WHERE pk_field_set=?",
                Int::class.java, set.id)
        assertEquals(1, count)

        count = jdbc.queryForObject("SELECT COUNT(1) FROM field_set_member WHERE pk_field=?",
                Int::class.java, field.id)
        assertEquals(1, count)
    }

    @Test
    fun testGet() {
        val spec = FieldSetSpec("The Shire")
        val fs1 = fieldSetDao.create(spec)
        val fs2 = fieldSetDao.get(fs1.id)
        assertEquals(fs1.id, fs2.id)
        assertEquals(fs1.name, fs2.name)
    }


    @Test
    fun testGetAll() {
        fieldSetDao.deleteAll()
        val spec = FieldSetSpec("The Shire")
        val fs1 = fieldSetDao.create(spec)
        assertEquals(1, fieldSetDao.getAll().size)
    }
}