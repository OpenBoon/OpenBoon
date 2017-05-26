package com.zorroa.archivist.repository;

import com.zorroa.archivist.JdbcUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.util.Properties;

/**
 * Created by chambers on 5/26/17.
 */
@Repository
public class StartupDaoImpl extends AbstractDao implements StartupDao {

    private static final String INSERT =
            JdbcUtils.insert("startup",
                    "time_started",
                    "str_version");

    @Override
    public int create() {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            String version = "unknown";
            try {
                Properties props = new Properties();
                props.load(new ClassPathResource("version.properties").getInputStream());
                version = props.getProperty("build.version");

            } catch (IOException e) {
                logger.warn("Unable to determine archivist version, ", e);
            }

            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_startup"});
            ps.setLong(1, System.currentTimeMillis());
            ps.setString(2, version);
            return ps;
        }, key);

        return key.getKey().intValue();
    }

}
