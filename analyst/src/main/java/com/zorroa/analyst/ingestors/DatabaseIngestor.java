package com.zorroa.analyst.ingestors;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.Argument;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by chambers on 4/26/16.
 */
public class DatabaseIngestor extends IngestProcessor {

    @Argument
    public String query;
    @Argument
    public String namespace;

    @Argument
    public String connectionUri;
    @Argument
    public String driverClass;
    @Argument
    public String username;
    @Argument
    public String password;

    private JdbcTemplate jdbc;

    @Override
    public void init() {
        setupDatasource();
    }

    private void setupDatasource() {
        PoolProperties props = new PoolProperties();
        props.setUrl(connectionUri);
        props.setDriverClassName(driverClass);
        props.setUsername(username);
        if (password != null) {
            props.setPassword(password);
        }
        props.setMaxActive(8);
        props.setInitialSize(4);

        org.apache.tomcat.jdbc.pool.DataSource ds = new org.apache.tomcat.jdbc.pool.DataSource();
        ds.setPoolProperties(props);

        jdbc = new JdbcTemplate(ds);
    }

    private static final Pattern PLACEHOLDER_REGEX = Pattern.compile("\\$\\{(.+)\\}");

    @Override
    public void process(AssetBuilder asset) {
        List<Object> args = Lists.newArrayList();
        StringBuffer sb = new StringBuffer();
        Matcher m = PLACEHOLDER_REGEX.matcher(query);
        while (m.find()) {
            m.appendReplacement(sb, "?");
            args.add(asset.getAttr(m.group(1)));
        }
        m.appendTail(sb);

        String newQuery = sb.toString();
        List<Map<String,Object>> result = jdbc.queryForList(newQuery, args.toArray());
        if (result.size() == 1) {
            asset.setAttr(namespace, result.get(0));
        }
        else if (result.size() > 1) {
            asset.setAttr(namespace, result);
        }
    }

    public JdbcTemplate getJdbc() {
        return jdbc;
    }

    public DatabaseIngestor setJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        return this;
    }
}
