package com.zorroa.archivist.repository

import com.google.common.collect.ImmutableSet
import org.springframework.stereotype.Repository

interface FieldDao {

    fun getHiddenFields(): Set<String>

    fun hideField(name: String, manual: Boolean): Boolean

    fun unhideField(name: String): Boolean
}

@Repository
class FieldDaoImpl : AbstractDao(), FieldDao {

    override fun hideField(name: String, manual: Boolean): Boolean {
        return if (isDbVendor("postgresql")) {
            jdbc.update(
                    "INSERT INTO field_hide (pk_field, bool_manual) VALUES (?, ?) ON CONFLICT(pk_field) DO NOTHING",
                    name, manual) == 1
        } else {
            jdbc.update("MERGE INTO field_hide (pk_field, bool_manual) KEY(pk_field) VALUES (?, ?)", name, manual) == 1
        }
    }

    override fun unhideField(name: String): Boolean {
        return jdbc.update("DELETE from field_hide WHERE pk_field=?", name) == 1
    }

    override fun getHiddenFields(): Set<String> {
        return ImmutableSet.copyOf(jdbc.queryForList("SELECT pk_field from field_hide", String::class.java))
    }
}
