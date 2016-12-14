package com.zorroa.common.elastic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.compress.CompressedXContent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.script.expression.ExpressionPlugin;
import org.elasticsearch.script.groovy.GroovyPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by chambers on 4/15/16.
 */
public class ElasticClientUtils {

    private static final Logger logger = LoggerFactory.getLogger(ElasticClientUtils.class);

    /**
     * Initialize and return an Elastic Client with the given settings.
     * @param builder
     * @return
     */
    public static Client initializeClient(Settings.Builder builder) {
        /*
         * Make sure groovy indexed scripts are always enabled.
         */
        builder.put("path.plugins", "{path.home}/es-plugins");
        builder.put("index.unassigned.node_left.delayed_timeout", "5m");
        builder.put("script.inline", true);
        builder.put("script.indexed", true);
        builder.put("script.engine.expression.indexed.update", true);
        builder.put("script.engine.groovy.indexed.update", true);

        Node node = new ZorroaNode(builder.build(),
                ImmutableSet.of(HammingDistancePlugin.class,
                        ArchivistDateScriptPlugin.class,
                        ExpressionPlugin.class,
                        GroovyPlugin.class));
        node.start();
        return node.client();
    }

    /**
     * Create the event log template document.
     *
     * @param client
     * @throws IOException
     */
    public static void createEventLogTemplate(Client client) throws IOException {
        ClassPathResource resource = new ClassPathResource("eventlog-template.json");
        byte[] source = ByteStreams.toByteArray(resource.getInputStream());
        PutIndexTemplateResponse rsp = client.admin().indices()
                .preparePutTemplate("eventlog").setSource(source).get();
        if (!rsp.isAcknowledged()) {
            logger.warn("Creating eventlog template not acked");
        }
    }

    /**
     * Create all indexed scripts.
     *
     * @param client
     */
    public static void createIndexedScripts(Client client) {
        createRemoveLinkScript(client);
        createAppendLinkScript(client);
        createAppendPermissionScript(client);
        createRemovePermissionScript(client);
    }

    private static final String REMOVE_SCRIPT =
            "if (ctx._source.links == null) { return; }; "+
            "if (ctx._source.links[type] == null) { return; }; "+
            "ctx._source.links[type].removeIf({i-> i==id});";

    public static void createRemoveLinkScript(Client client) {
        Map<String, Object> script = ImmutableMap.of(
                "script", REMOVE_SCRIPT,
                "params", ImmutableMap.of("type", "type", "id", "id"));

        client.preparePutIndexedScript()
                .setScriptLang("groovy")
                .setId("remove_link")
                .setSource(script)
                .get();
    }

    private static final String APPEND_SCRIPT =
            "if (ctx._source.links == null) { ctx._source.links = [:]; };  " +
            "if (ctx._source.links[type] == null) { ctx._source.links[type] = []; };" +
            "ctx._source.links[type] += id; "+
            "ctx._source.links[type] = ctx._source.links[type].unique();";

    public static void createAppendLinkScript(Client client) {
        Map<String, Object> script = ImmutableMap.of(
                "script", APPEND_SCRIPT,
                "params", ImmutableMap.of("type", "type", "id", "id"));

        client.preparePutIndexedScript()
                .setScriptLang("groovy")
                .setId("append_link")
                .setSource(script)
                .get();
    }

    private static final String REMOVE_PERM_SCRIPT =
            "if (ctx._source.permissions == null) { return; }; "+
                    "if (ctx._source.permissions[type] == null) { return; }; "+
                    "ctx._source.permissions[type].removeIf({i-> i==id});";

    public static void createRemovePermissionScript(Client client) {
        Map<String, Object> script = ImmutableMap.of(
                "script", REMOVE_PERM_SCRIPT,
                "params", ImmutableMap.of("type", "type", "id", "id"));

        client.preparePutIndexedScript()
                .setScriptLang("groovy")
                .setId("remove_permission")
                .setSource(script)
                .get();
    }

    private static final String APPEND_PERM_SCRIPT =
            "if (ctx._source.permissions == null) { ctx._source.permissions = [:]; };  " +
                    "if (ctx._source.permissions[type] == null) { ctx._source.permissions[type] = []; };" +
                    "ctx._source.permissions[type] += id; "+
                    "ctx._source.permissions[type] = ctx._source.permissions[type].unique();";

    public static void createAppendPermissionScript(Client client) {
        Map<String, Object> script = ImmutableMap.of(
                "script", APPEND_PERM_SCRIPT,
                "params", ImmutableMap.of("type", "type", "id", "id"));

        client.preparePutIndexedScript()
                .setScriptLang("groovy")
                .setId("append_permission")
                .setSource(script)
                .get();
    }

    /**
     * Delete all indexes.
     *
     * @param client
     */
    public static void deleteAllIndexes(Client client) {
        client.admin().indices().prepareDelete("_all").get();
    }

    /**
     * Refresh all indexes.
     *
     * @param client
     */
    public static void refreshIndex(Client client) {
        refreshIndex(client, 10);
    }

    /**
     * Refresh all indexes and sleep.
     *
     * @param client
     * @param sleep
     */
    public static void refreshIndex(Client client, long sleep) {
        try {
            Thread.sleep(sleep/2);
        } catch (InterruptedException e) {
        }
        client.admin().indices().prepareRefresh("_all").get();
        try {
            Thread.sleep(sleep/2);
        } catch (InterruptedException e) {
        }
    }

    private static final Pattern MAPPING_NAMING_CONV = Pattern.compile("^V(\\d+)__(.*?).json$");

    /**
     * Create the latest mapping found with the given search path.
     *
     * @param client
     * @param index
     * @param searchPath
     * @throws IOException
     */
    public static void createLatestMapping(Client client, String index, String searchPath) throws IOException {
        Resource mapping = getLatestMappingVersion(searchPath);
        client.admin()
                .indices()
                .prepareCreate(index)
                .setSource(ByteStreams.toByteArray(mapping.getInputStream()))
                .get();
    }

    /**
     * Create the latest mapping found in the default search path: classpath:/db/mappings/*.json
     *
     * @param client
     * @param index
     * @throws IOException
     */
    public static void createLatestMapping(Client client, String index) throws IOException {
        Resource mapping = getLatestMappingVersion("classpath:/db/mappings/" + index + "/*.json");
        client.admin()
                .indices()
                .prepareCreate(index)
                .setSource(ByteStreams.toByteArray(mapping.getInputStream()))
                .get();
    }

    /**
     * Get the latest version of the mapping found at the given path.
     *
     * @param searchPath
     * @return
     * @throws IOException
     */
    public static Resource getLatestMappingVersion(String searchPath) throws IOException {

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(
                ElasticClientUtils.class.getClassLoader());
        Resource[] resources = resolver.getResources(searchPath);

        Resource latestVersion = null;
        int latestVerionNumber = 0;

        for (Resource resource : resources) {
            Matcher matcher = MAPPING_NAMING_CONV.matcher(resource.getFilename());
            if (!matcher.matches()) {
                logger.warn("'{}' is not using the proper naming convention.", resource.getFilename());
                continue;
            }

            int version = Integer.valueOf(matcher.group(1));
            if (version > latestVerionNumber) {
                latestVerionNumber = version;
                latestVersion = resource;
            }
        }
        logger.info("Loading latest elastic version: {}", latestVerionNumber);
        return latestVersion;
    }

    /**
     * Return the elastic type for any field.
     *
     * @param client
     * @param index
     * @param type
     * @param field
     * @return
     */
    public static String getFieldType(Client client, String index, String type, String field) {
        ClusterState cs = client.admin().cluster().prepareState().execute().actionGet().getState();
        IndexMetaData imd =
                cs.getMetaData().getAliasAndIndexLookup().get(index).getIndices().get(0);
        MappingMetaData mmd = imd.mapping(type);
        CompressedXContent source = mmd.source();
        try {

            JsonNode mappingNode = new ObjectMapper().readTree(source.uncompressed());
            JsonNode propertiesNode = mappingNode.get("asset");

            for (String f: Splitter.on('.').split(field)) {
                propertiesNode = propertiesNode.get("properties").get(f);
            }

            if (propertiesNode != null) {
                return propertiesNode.get("type").asText();
            }

        } catch (Exception e) {
            logger.warn("Failed to determine type of field: {}, assuming string.", field);
        }

        return "string";
    }
}
