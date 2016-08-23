package com.zorroa.archivist.repository;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

public class AbstractDao {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected JdbcTemplate jdbc;

    @Autowired
    public void setDatasource(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    private LoadingCache<Integer, String> cachedUserName = CacheBuilder.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .initialCapacity(32)
            .maximumSize(512)
            .build(new CacheLoader<Integer, String>() {
                @Override
                public String load(Integer key) throws Exception {
                    return jdbc.queryForObject("SELECT str_username FROM user WHERE pk_user=?", String.class, key);
                }
            });

    protected String resolveUser(int id) {
        try {
            return cachedUserName.get(id);
        } catch (Exception e) {
            return "unknown";
        }
    }
}
