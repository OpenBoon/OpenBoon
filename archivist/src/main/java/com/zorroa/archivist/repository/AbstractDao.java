package com.zorroa.archivist.repository;

import com.zorroa.common.config.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class AbstractDao {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected JdbcTemplate jdbc;

    protected ApplicationProperties properties;

    protected String dbVendor;

    public boolean isDbVendor(String vendor) {
        return dbVendor.equals(vendor);
    }

    @Autowired
    public void setApplicationProperties(ApplicationProperties properties) {
        this.properties = properties;
        this.dbVendor = properties.getString("archivist.datasource.primary.vendor");
    }

    @Autowired
    public void setDatasource(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }
}
