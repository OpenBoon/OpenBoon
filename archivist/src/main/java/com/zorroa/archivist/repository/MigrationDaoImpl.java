package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Migration;
import com.zorroa.archivist.domain.MigrationType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by chambers on 2/2/16.
 */
@Repository
public class MigrationDaoImpl extends AbstractDao implements MigrationDao {

    private static final RowMapper<Migration> MAPPER = (rs, row) -> {
        Migration m = new Migration();
        m.setId(rs.getInt("pk_migration"));
        m.setName(rs.getString("str_name"));
        m.setType(MigrationType.values()[rs.getInt("int_type")]);
        m.setPath(rs.getString("str_path"));
        m.setVersion(rs.getInt("int_version"));
        return m;
    };

    private static final String GET =
            "SELECT " +
                "pk_migration,"+
                "str_name,"+
                "int_type,"+
                "str_path,"+
                "int_version " +
            "FROM "+
                "migration ";

    @Override
    public List<Migration> getAll() {
        return jdbc.query(GET, MAPPER);
    }

    @Override
    public List<Migration> getAll(MigrationType type) {
        return jdbc.query(GET + " WHERE int_type=?", MAPPER, type.ordinal());
    }

    @Override
    public boolean setVersion(Migration m, int version) {
        return jdbc.update("UPDATE migration SET int_version=? WHERE pk_migration=? AND int_version!=?",
                version, m.getId(), version) == 1;
    }
}
