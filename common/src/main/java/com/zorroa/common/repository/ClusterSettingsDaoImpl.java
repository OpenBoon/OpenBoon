package com.zorroa.common.repository;

import com.google.common.collect.Maps;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.elastic.AbstractElasticDao;
import com.zorroa.sdk.util.FileUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.unit.TimeValue;

import java.nio.file.Paths;
import java.util.Map;

/**
 * Created by chambers on 7/8/16.
 */
public class ClusterSettingsDaoImpl extends AbstractElasticDao implements ClusterSettingsDao {

    private static final String ID = "settings";

    @Override
    public String getType() {
        return "cluster";
    }

    @Override
    public String getIndex() {
        return "analyst";
    }

    @Override
    public Map<String, Object> get() {
        Map<String,Object> config = client.prepareGet(getIndex(), getType(), ID)
                .get(TimeValue.timeValueSeconds(10))
                .getSourceAsMap();
        Map<String,Object> result = Maps.newHashMapWithExpectedSize(config.size());
        config.forEach((k,v)-> {
            result.put(k.replace(DELIMITER, "."), v);
        });
        return config;
    }

    @Override
    public void load() {
        Map<String, Object> config = client.prepareGet(getIndex(), getType(), ID)
                .get(TimeValue.timeValueSeconds(10))
                .getSourceAsMap();
        config.forEach((k,v)-> {
            k = k.replace(DELIMITER, ".");
            System.setProperty(k.replace(DELIMITER, "."), v.toString());
            logger.info("Setting cluster property {}={}", k, v);
        });
    }

    @Override
    public void save(ApplicationProperties properties) {
        Map<String, Object> source = Maps.newHashMap();
        properties.getMap("zorroa.cluster.").forEach((k,v)-> {
            if (k.contains(".path.")) {
                v = FileUtils.normalize(Paths.get(v.toString())).toString();
            }
            source.put(k.replace(".", DELIMITER), v);
        });

        client.prepareIndex(getIndex(), getType(), ID)
                .setOpType(IndexRequest.OpType.INDEX)
                .setSource(source)
                .setRefresh(true)
                .get(TimeValue.timeValueSeconds(10));
    }

}
