package com.zorroa.archivist.repository

import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.Request
import com.zorroa.archivist.domain.RequestSpec
import com.zorroa.archivist.domain.RequestState
import com.zorroa.archivist.domain.RequestType
import com.zorroa.archivist.security.SecurityUtils
import com.zorroa.sdk.util.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository

interface RequestDao {
    fun create(spec: RequestSpec) : Request
    fun get(id: Int) : Request
}

@Repository
class RequestDaoImpl : AbstractDao(), RequestDao {

    @Autowired
    internal lateinit var userDaoCache: UserDaoCache;

    private val MAPPER = RowMapper<Request> { rs, _ ->
         Request(
                 rs.getInt("pk_request"),
                 rs.getInt("pk_folder"),
                 RequestType.values()[rs.getInt("int_type")],
                 userDaoCache.getUser(rs.getInt("user_created")),
                 rs.getLong("time_created"),
                 userDaoCache.getUser(rs.getInt("user_modified")),
                 rs.getLong("time_modified"),
                 RequestState.values()[rs.getInt("int_state")],
                 rs.getString("str_comment"),
                 Json.deserialize(rs.getString("json_cc"), Json.LIST_OF_STRINGS)
         )
    }

    override fun create(spec: RequestSpec) : Request {
        val keyHolder = GeneratedKeyHolder()

        jdbc.update({ connection ->
            val userId = SecurityUtils.getUser().id
            val time = System.currentTimeMillis()
            val ps = connection.prepareStatement(INSERT, arrayOf("pk_request"))
            ps.setInt(1, userId)
            ps.setInt(2, userId)
            ps.setLong(3, time)
            ps.setLong(4, time)
            ps.setInt(5, spec.folderId!!)
            ps.setInt(6, spec.type!!.ordinal)
            ps.setString(7, spec.comment)
            ps.setString(8, Json.serializeToString(spec.emailCC, "[]"))
            ps
        }, keyHolder)

        val id = keyHolder.key.toInt()
        return get(id)
    }

    override fun get(id: Int) : Request {
        return jdbc.queryForObject("$GET WHERE pk_request=?", MAPPER, id)
    }

    companion object {

        private val GET = "SELECT " +
                "pk_request,"+
                "pk_folder,"+
                "user_created,"+
                "user_modified,"+
                "time_created,"+
                "time_modified,"+
                "int_count,"+
                "int_type,"+
                "int_state,"+
                "str_comment, "+
                "json_cc "+
        "FROM "+
            "request"

        private val INSERT = JdbcUtils.insert("request",
                "user_created",
                "user_modified",
                "time_created",
                "time_modified",
                "pk_folder",
                "int_type",
                "str_comment",
                "json_cc")
    }
}
