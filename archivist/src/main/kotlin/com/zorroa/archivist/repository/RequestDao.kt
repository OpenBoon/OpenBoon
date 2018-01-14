package com.zorroa.archivist.repository

import com.google.common.base.Preconditions
import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.Request
import com.zorroa.archivist.domain.RequestSpec
import com.zorroa.archivist.domain.RequestType
import com.zorroa.archivist.security.SecurityUtils
import com.zorroa.sdk.search.AssetSearch
import com.zorroa.sdk.util.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository

interface RequestDao {

}

@Repository
class RequestDaoImpl : AbstractDao(), RequestDao {

    @Autowired
    internal var userDaoCache: UserDaoCache? = null

    private val MAPPER = RowMapper<Request> { rs, _ ->
        val req = Request().apply {
            id = rs.getInt("pk_request")
            search = Json.deserialize(rs.getString("str_search"), AssetSearch::class.java)
            userCreated = userDaoCache?.getUser(rs.getInt("user_created"))
            timeCreated = rs.getLong("time_created")
            types = rs.getObject("int_types") as Array<RequestType>
        }
        req
    }


    fun create(spec: RequestSpec) : Request {
        Preconditions.checkNotNull(spec.search, "The search for a request cannot be null")
        Preconditions.checkNotNull(spec.type, "The types for a request cannot be null")
        Preconditions.checkNotNull(spec.comment, "The comment for a request cannot be null")

        val keyHolder = GeneratedKeyHolder()

        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT, arrayOf("pk_request"))
            ps.setInt(1, SecurityUtils.getUser().id)
            ps.setLong(2, System.currentTimeMillis())
            ps.setString(3, Json.serializeToString(spec.search))
            ps.setInt(4, spec.count)
            ps
        }, keyHolder)

        val id = keyHolder.key.toInt()
        return get(id)
    }

    fun get(id: Int) : Request {
        return jdbc.queryForObject("$GET WHERE pk_request=?", MAPPER, id)
    }

    companion object {

        private val GET = "SELECT " +
                "pk_request,"+
                "user_created,"+
                "time_created,"+
                "json_search,"+
                "int_count,"+
                "int_types,"+
                "str_comment "+
        "FROM "+
            "request";

        private val INSERT = JdbcUtils.insert("request",
                "user_created",
                "time_created",
                "json_search",
                "int_count",
                "int_types",
                "str_comment")

    }

}
