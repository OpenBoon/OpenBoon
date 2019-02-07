package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.*
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.service.event
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.JdbcUtils
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.util.*

interface FieldSetDao {

    fun get(id: UUID) : FieldSet
    fun create(spec: FieldSetSpec) : FieldSet
    fun setMembers(fieldSet: FieldSet, members: List<UUID>) : Int
    fun getAll() : List<FieldSet>
    fun getAll(doc: Document) : List<FieldSet>
    fun count(filter: FieldSetFilter): Long
    fun getAll(filter: FieldSetFilter?): KPagedList<FieldSet>
    fun deleteAll() : Int
}

@Repository
class FieldSetDaoImpl : AbstractDao(), FieldSetDao  {

    override fun create(spec: FieldSetSpec) : FieldSet {
        val time = System.currentTimeMillis()
        val id = uuid1.generate()
        val user = getUser()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, user.organizationId)
            ps.setObject(3, user.id)
            ps.setObject(4, user.id)
            ps.setLong(5, time)
            ps.setLong(6, time)
            ps.setString(7, spec.name)
            ps.setString(8, spec.linkExpression)
            ps
        }

        logger.event(LogObject.FIELD_SET, LogAction.CREATE,
                mapOf("fieldSetId" to id, "fieldSetName" to spec.name))

        val fs = FieldSet(id, spec.name, spec.linkExpression)
        spec.fieldIds?.let {
            setMembers(fs, it)
        }
        return fs
    }

    override fun get(id: UUID) : FieldSet {
        return jdbc.queryForObject("$GET WHERE pk_organization=? AND pk_field_set=?",
                MAPPER, getOrgId(), id)
    }

    override fun getAll() : List<FieldSet> {
        return jdbc.query("$GET WHERE pk_organization=? ORDER BY str_name ASC", MAPPER, getOrgId())
    }

    override fun getAll(doc: Document) : List<FieldSet> {
        val result = mutableListOf<FieldSet>()
        var linkExpresionMatches = false
        var currentFieldSet : FieldSet? =  null

        jdbc.query(GET_RESOLVED, RowCallbackHandler { rs ->
            val fsId = rs.getObject("pk_field_set") as UUID

            if (currentFieldSet == null || currentFieldSet?.id != fsId) {
                val fs = FieldSet(
                        fsId,
                        rs.getString("str_name"),
                        rs.getString("str_link_expr"),
                        mutableListOf())

                currentFieldSet = fs
                linkExpresionMatches = checkLinkExpr(doc, fs.linkExpression)
                if (linkExpresionMatches) {
                    result.add(fs)
                }
            }

            if (linkExpresionMatches) {
                currentFieldSet?.fields?.let {
                    it.add(Field(
                            rs.getObject("field_id") as UUID,
                            rs.getString("field_name"),
                            rs.getString("field_attr_name"),
                            AttrType.values()[rs.getInt("field_attr_type")],
                            rs.getBoolean("field_editable"),
                            rs.getBoolean("field_custom"),
                            doc.getAttr(rs.getString("field_attr_name"), Any::class.java),
                            rs.getObject("pk_field_edit") as UUID?))
                }
            }

        }, doc.id, getOrgId())

        return result
    }

    override fun getAll(filter: FieldSetFilter?): KPagedList<FieldSet> {
        val filt = filter ?: FieldSetFilter()
        val query = filt.getQuery(GET, false)
        val values = filt.getValues(false)
        return KPagedList(count(filt), filt.page, jdbc.query(query, MAPPER, *values))
    }

    override fun count(filter: FieldSetFilter): Long {
        val query = filter.getQuery(COUNT, true)
        return jdbc.queryForObject(query, Long::class.java, *filter.getValues(true))
    }

    override fun setMembers(fieldSet: FieldSet, members: List<UUID>) : Int {
        jdbc.update("DELETE FROM field_set_member WHERE pk_field_set=?", fieldSet.id)

        return jdbc.batchUpdate(INSERT_MEMBER, object : BatchPreparedStatementSetter {
            override fun setValues(ps: PreparedStatement, idx: Int) {
                val fieldId = members[idx]
                ps.setObject(1, uuid1.generate())
                ps.setObject(2, fieldId)
                ps.setObject(3, fieldSet.id)
                ps.setInt(4, idx)
            }

            override fun getBatchSize(): Int = members.size
        }).sum()
    }

    override fun deleteAll() : Int {
        return jdbc.update("DELETE FROM field_set WHERE pk_organization=?", getOrgId())
    }

    fun checkLinkExpr(doc: Document, expr: String?) : Boolean {
        if (expr == null) {
            return true
        }

        return try {
            val parts = expr.split(":", limit = 2)
            when {
                parts[0] == "_exists_" -> doc.attrExists(parts[1])
                parts[0] == "_empty_" -> doc.isEmpty(parts[1])
                parts[0] == "_not_exists_" -> !doc.attrExists(parts[1])
                parts[0] == "_not_empty_" -> !doc.isEmpty(parts[1])
                else -> doc.getAttr(parts[0], String::class.java) == parts[1]
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse link expr: '$expr'")
            true
        }
    }


    private val MAPPER = RowMapper { rs, _ ->
        FieldSet(
                rs.getObject("pk_field_set") as UUID,
                rs.getString("str_name"),
                rs.getString("str_link_expr"))
    }

    companion object {

        private val INSERT = JdbcUtils.insert("field_set",
                "pk_field_set",
                "pk_organization",
                "pk_user_created",
                "pk_user_modified",
                "time_created",
                "time_modified",
                "str_name",
                "str_link_expr")

        private const val GET = "SELECT * FROM field_set"
        private const val COUNT = "SELECT COUNT(1) FROM field_set"

        private val INSERT_MEMBER = JdbcUtils.insert("field_set_member",
                "pk_field_set_member",
                "pk_field",
                "pk_field_set",
                "int_order")


        private const val GET_RESOLVED = "SELECT " +
                "field_set.pk_field_set," +
                "field_set.str_name,"+
                "field_set.str_link_expr,"+
                "field.pk_field AS field_id," +
                "field.str_name AS field_name,"+
                "field.str_attr_name AS field_attr_name,"+
                "field.int_attr_type AS field_attr_type, " +
                "field.bool_custom AS field_custom," +
                "field.bool_editable AS field_editable, " +
                "field_edit.pk_field_edit " +
                "FROM " +
                    "field_set " +
                    "INNER JOIN field_set_member fsm ON (field_set.pk_field_set = fsm.pk_field_set) " +
                    "INNER JOIN field ON (fsm.pk_field = field.pk_field) " +
                    "LEFT JOIN field_edit ON (field.pk_field = field_edit.pk_field AND field_edit.pk_asset=?::uuid) " +
                "WHERE " +
                    "field_set.pk_organization=?::uuid " +
                "ORDER BY " +
                    "field_set.pk_field_set ASC," +
                    "field.str_name ASC"


    }
}