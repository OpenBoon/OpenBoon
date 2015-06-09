package com.zorroa.archivist.repository;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class AbstractDao {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractDao.class);

    protected JdbcTemplate jdbc;

    @Autowired
    public void setDatasource(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }
}
