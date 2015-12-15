package com.zorroa.archivist.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class AbstractDao {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected JdbcTemplate jdbc;

    @Autowired
    public void setDatasource(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }
}
