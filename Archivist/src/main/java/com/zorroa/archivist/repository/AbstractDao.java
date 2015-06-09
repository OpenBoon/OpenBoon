package com.zorroa.archivist.repository;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class AbstractDao {

    protected JdbcTemplate jdbc;

    @Autowired
    public void setDatasource(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }
}
