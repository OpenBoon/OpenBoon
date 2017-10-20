package com.zorroa.archivist.repository;

import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.Taxonomy;
import com.zorroa.archivist.domain.TaxonomySpec;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
public class TaxonomyDaoImpl extends AbstractDao implements TaxonomyDao {


    private static final String INSERT =
            JdbcUtils.insert("taxonomy",
                    "pk_folder");

    @Override
    public Taxonomy create(TaxonomySpec spec) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_taxonomy"});
            ps.setInt(1, spec.getFolderId());
            return ps;
        }, keyHolder);
        return get(keyHolder.getKey().intValue());
    }

    private static final String GET =
            "SELECT " +
                "pk_taxonomy," +
                "pk_folder, " +
                "bool_active,"+
                "time_started,"+
                "time_stopped "+
            "FROM " +
                "taxonomy ";

    private static final RowMapper<Taxonomy> MAPPER = (rs, row) -> {
        Taxonomy tax = new Taxonomy();
        tax.setFolderId(rs.getInt("pk_folder"));
        tax.setTaxonomyId(rs.getInt("pk_taxonomy"));
        tax.setActive(rs.getBoolean("bool_active"));
        tax.setTimeStarted(rs.getLong("time_started"));
        tax.setTimeStopped(rs.getLong("time_stopped"));

        return tax;
    };

    @Override
    public Taxonomy get(int id) {
        return jdbc.queryForObject(GET.concat("WHERE pk_taxonomy=?"), MAPPER, id);
    }

    @Override
    public Taxonomy get(Folder folder) {
        return jdbc.queryForObject(GET.concat("WHERE pk_folder=?"), MAPPER, folder.getId());
    }

    @Override
    public Taxonomy refresh(Taxonomy object) {
        return get(object.getTaxonomyId());
    }

    @Override
    public List<Taxonomy> getAll() {
        return jdbc.query(GET, MAPPER);
    }

    @Override
    public PagedList<Taxonomy> getAll(Pager paging) {
        return null;
    }

    @Override
    public boolean update(int id, Taxonomy spec) {
        return false;
    }

    @Override
    public boolean setActive(Taxonomy id, boolean value) {
        if (value) {
            return jdbc.update("UPDATE taxonomy SET time_started=?, time_stopped=0, bool_active=? WHERE pk_taxonomy=? AND bool_active=?",
                    System.currentTimeMillis(), true, id.getTaxonomyId(), false) == 1;
        }
        else {
            return jdbc.update("UPDATE taxonomy SET time_stopped=?, bool_active=? WHERE pk_taxonomy=? AND bool_active=?",
                    System.currentTimeMillis(), false, id.getTaxonomyId(), true) == 1;
        }
    }

    @Override
    public boolean delete(int id) {
        return jdbc.update("DELETE FROM taxonomy WHERE pk_taxonomy=?", id) == 1;
    }

    @Override
    public long count() {
        return 0;
    }
}
