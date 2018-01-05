package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.util.Json;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
public class BlobDaoImpl extends AbstractDao implements BlobDao {

    private static final String INSERT =
            JdbcUtils.insert("jblob",
                    "str_app",
                    "str_feature",
                    "str_name",
                    "json_data",
                    "user_created",
                    "user_modified",
                    "time_created",
                    "time_modified");

    @Override
    public Blob create(String app, String feature, String name, Object data) {
        final long time = System.currentTimeMillis();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_jblob"});
            ps.setString(1, app);
            ps.setString(2, feature);
            ps.setString(3, name);
            ps.setString(4, Json.serializeToString(data, "{}"));
            ps.setInt(5, SecurityUtils.getUser().getId());
            ps.setInt(6, SecurityUtils.getUser().getId());
            ps.setLong(7, time);
            ps.setLong(8, time);
            return ps;
        }, keyHolder);

        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    private final RowMapper<Blob> MAPPER = (rs, row) -> {
        Blob blob = new Blob();
        blob.setBlobId(rs.getInt("pk_jblob"));
        blob.setVersion(rs.getLong("int_version"));
        blob.setAcl(null);
        blob.setApp(rs.getString("str_app"));
        blob.setFeature(rs.getString("str_feature"));
        blob.setName(rs.getString("str_name"));
        blob.setData(Json.deserialize(rs.getString("json_data"), Json.GENERIC_MAP));
        return blob;
    };

    private static final String UPDATE =
            "UPDATE " +
                    "jblob " +
            "SET " +
                    "json_data=?,"+
                    "int_version=int_version+1,"+
                    "user_modified=?, "+
                    "time_modified=? " +
            "WHERE " +
                    "str_app=? AND str_feature=? AND str_name=? ";

    @Override
    public boolean update(BlobId bid, Object data) {
        return jdbc.update(UPDATE,
                Json.serializeToString(data, "{}"), SecurityUtils.getUser().getId(), System.currentTimeMillis(),
                bid.getBlobId()) == 1;
    }

    @Override
    public boolean delete(BlobId blob) {
        return jdbc.update("DELETE FROM jblob WHERE pk_jblob=?", blob.getBlobId()) == 1;
    }

    private static final String GET =
            "SELECT " +
                "pk_jblob,"+
                "str_app,"+
                "str_feature,"+
                "str_name,"+
                "json_data,"+
                "int_version "+
            "FROM " +
                "jblob ";

    @Override
    public Blob get(int id) {
        return jdbc.queryForObject(appendAccess(GET.concat("WHERE pk_jblob=?"), Access.Read), MAPPER,
                        appendAccessArgs(id));
    }

    @Override
    public Blob get(String app, String feature, String id) {
        return jdbc.queryForObject(appendAccess(
                GET.concat("WHERE str_app=? AND str_feature=? AND str_name=?"), Access.Read), MAPPER,
                appendAccessArgs(app, feature, id));
    }

    @Override
    public BlobId getId(String app, String feature, String name, Access forAccess) {
        return jdbc.queryForObject(
                appendAccess("SELECT pk_jblob FROM jblob WHERE str_app=? AND str_feature=? AND str_name=?", forAccess),
            (rs, i) -> {
                final int blobId = rs.getInt(1);
                return () -> blobId;
            }, appendAccessArgs(app, feature, name));
    }

    @Override
    public List<Blob> getAll(String app, String feature) {
        return jdbc.query(GET.concat("WHERE str_app=? AND str_feature=?"), MAPPER,
                app, feature);
    }

    @Override
    public Acl getPermissions(BlobId blob) {
        final Acl acl = new Acl();
        jdbc.query("SELECT jblob_acl.pk_permission, jblob_acl.int_access, permission.str_name FROM jblob_acl, permission WHERE " +
                "permission.pk_permission = jblob_acl.pk_permission AND pk_jblob=?", rs -> {
            acl.addEntry(rs.getInt("pk_permission"), rs.getInt("int_access"));
        }, blob.getBlobId());

        return acl;
    }

    @Override
    public Acl setPermissions(BlobId blob, SetPermissions req) {

        if (req.replace) {
            jdbc.update("DELETE FROM jblob_acl WHERE pk_jblob=?", blob.getBlobId());
            for (AclEntry entry: req.acl) {
                addPermission(blob, entry);
            }
        }
        else {
            for (AclEntry entry: req.acl) {
                if (entry.getAccess() > 7) {
                    throw new ArchivistWriteException("Invalid Access level "
                            + entry.getAccess() + " for permission ID " + entry.getPermissionId());
                }
                if (entry.access <= 0) {
                    jdbc.update("DELETE FROM jblob_acl WHERE pk_permission=?", entry.permissionId);
                }
                else {
                    if (jdbc.update("UPDATE jblob_acl SET int_access=? WHERE pk_jblob=? AND pk_permission=?",
                            entry.access, blob.getBlobId(), entry.getPermissionId()) != 1) {
                        addPermission(blob, entry);
                    }
                }
            }
        }

        return getPermissions(blob);
    }

    private void addPermission(BlobId blob, AclEntry entry) {
        jdbc.update("INSERT INTO jblob_acl (pk_jblob, pk_permission, int_access) VALUES (?,?,?)",
                blob.getBlobId(), entry.permissionId, entry.access);
    }

    public Object[] appendAccessArgs(Object ... args) {
        if (SecurityUtils.hasPermission("group::administrator"))  {
            return args;
        }

        List<Object> result = Lists.newArrayListWithCapacity(args.length + SecurityUtils.getPermissionIds().size());
        for (Object a: args) {
            result.add(a);
        }
        result.add(SecurityUtils.getUser().getId());
        result.addAll(SecurityUtils.getPermissionIds());
        return result.toArray();
    }

    private String appendAccess(String query, Access access) {
        if (SecurityUtils.hasPermission("group::administrator")) {
            return query;
        }

        StringBuilder sb = new StringBuilder(query.length() + 256);
        sb.append(query);
        if (query.contains("WHERE")) {
            sb.append(" AND ");
        }
        else {
            sb.append(" WHERE ");
        }
        sb.append("(jblob.user_created = ? OR (");
        sb.append("SELECT COUNT(1) FROM jblob_acl WHERE jblob_acl.pk_jblob=jblob.pk_jblob AND ");
        sb.append(JdbcUtils.in("jblob_acl.pk_permission", SecurityUtils.getPermissionIds().size()));
        sb.append(" AND BITAND(");
        sb.append(access.getValue());
        sb.append(",int_access) = " + access.getValue() + ") > 0 OR (");
        sb.append("SELECT COUNT(1) FROM jblob_acl WHERE jblob_acl.pk_jblob=jblob.pk_jblob) = 0)");
        return sb.toString();
    }
}
