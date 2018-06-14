package com.zorroa.archivist.repository

import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.Request
import com.zorroa.archivist.domain.RequestSpec
import com.zorroa.archivist.domain.RequestState
import com.zorroa.archivist.domain.RequestType
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.security.getUserId
import com.zorroa.sdk.util.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface RequestDao {
    fun create(spec: RequestSpec) : Request
    fun get(id: UUID) : Request
}

@Repository
class RequestDaoImpl : AbstractDao(), RequestDao {

    @Autowired
    internal lateinit var userDaoCache: UserDaoCache;

    private val MAPPER = RowMapper<Request> { rs, _ ->
         Request(
                 rs.getObject("pk_request") as UUID,
                 rs.getObject("pk_folder") as UUID,
                 RequestType.values()[rs.getInt("int_type")],
                 userDaoCache.getUser(rs.getObject("pk_user_created") as UUID),
                 rs.getLong("time_created"),
                 userDaoCache.getUser(rs.getObject("pk_user_modified") as UUID),
                 rs.getLong("time_modified"),
                 RequestState.values()[rs.getInt("int_state")],
                 rs.getString("str_comment"),
                 Json.deserialize(rs.getString("json_cc"), Json.LIST_OF_STRINGS)
         )
    }

    override fun create(spec: RequestSpec) : Request {
        val id = uuid1.generate()
        val userId = getUserId()
        val time = System.currentTimeMillis()

        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, userId)
            ps.setObject(3, userId)
            ps.setLong(4, time)
            ps.setLong(5, time)
            ps.setObject(6, spec.folderId)
            ps.setInt(7, spec.type!!.ordinal)
            ps.setString(8, spec.comment)
            ps.setString(9, Json.serializeToString(spec.emailCC, "[]"))
            ps.setObject(10, getUser().organizationId)
            ps
        })

        return get(id)
    }

    override fun get(id: UUID) : Request {
        return jdbc.queryForObject("$GET WHERE pk_request=?", MAPPER, id)
    }

    companion object {

        private val GET = "SELECT " +
                "pk_request,"+
                "pk_folder,"+
                "pk_user_created,"+
                "pk_user_modified,"+
                "time_created,"+
                "time_modified,"+
                "int_type,"+
                "int_state,"+
                "str_comment, "+
                "json_cc "+
        "FROM "+
            "request"

        private val INSERT = JdbcUtils.insert("request",
                "pk_request",
                "pk_user_created",
                "pk_user_modified",
                "time_created",
                "time_modified",
                "pk_folder",
                "int_type",
                "str_comment",
                "json_cc",
                "pk_organization")
    }
}
