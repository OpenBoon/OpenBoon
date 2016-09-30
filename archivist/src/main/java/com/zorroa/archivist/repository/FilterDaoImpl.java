package com.zorroa.archivist.repository;

import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.Filter;
import com.zorroa.archivist.domain.FilterSpec;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * Created by chambers on 8/8/16.
 */
@Repository
public class FilterDaoImpl extends AbstractDao implements FilterDao {

    private static final String INSERT =
            JdbcUtils.insert("filter", "str_description", "bool_match_all", "bool_enabled");

    @Override
    public Filter create(FilterSpec spec) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_filter"});
            ps.setString(1, spec.getDescription());
            ps.setBoolean(2, spec.isMatchAll());
            ps.setBoolean(3, spec.isEnabled());
            return ps;
        }, keyHolder);

        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    private static final RowMapper<Filter> MAPPER = (rs, row) -> {
        Filter f = new Filter();
        f.setId(rs.getInt("pk_filter"));
        f.setDescription(rs.getString("str_description"));
        f.setEnabled(rs.getBoolean("bool_enabled"));
        f.setMatchAll(rs.getBoolean("bool_match_all"));
        return f;
    };

    private static final String GET =
            "SELECT " +
                "pk_filter,"+
                "str_description,"+
                "bool_enabled, " +
                "bool_match_all " +
            "FROM " +
                "filter ";

    @Override
    public Filter get(int id) {
        return jdbc.queryForObject(GET.concat("WHERE pk_filter=?"), MAPPER, id);
    }

    @Override
    public Filter refresh(Filter object) {
        return get(object.getId());
    }

    @Override
    public List<Filter> getAll() {
        return jdbc.query(GET, MAPPER);
    }

    @Override
    public PagedList<Filter> getAll(Pager page) {
        return new PagedList(page.setTotalCount(count()),
                jdbc.query(GET.concat(" ORDER BY str_description LIMIT ? OFFSET ?"),
                        MAPPER, page.getSize(), page.getFrom()));

    }

    @Override
    public boolean update(int id, Filter spec) {
        return false;
    }

    @Override
    public boolean delete(int id) {
        return jdbc.update("DELETE FROM filter WHERE pk_filter=?", id) == 1;
    }

    @Override
    public long count() {
        return jdbc.queryForObject("SELECT COUNT(1) FROM filter", Long.class);
    }
}
