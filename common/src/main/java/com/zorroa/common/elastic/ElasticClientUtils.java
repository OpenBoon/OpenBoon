package com.zorroa.common.elastic;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
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
        builder.put("script.engine.groovy.indexed.update", true);
        builder.put("path.plugins", "{path.home}/es-plugins");

        Node node = new ZorroaNode(builder.build(), ImmutableSet.of(ArchivistDateScriptPlugin.class));
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
        client.admin().indices().preparePutTemplate("eventlog").setSource(source).get();
    }

    /**
     * Create all indexed scripts.
     *
     * @param client
     */
    public static void createIndexedScripts(Client client) {
        Map<String, Object> script1 = ImmutableMap.of(
                "script", "if (ctx._source.exports == null ) {  ctx._source.exports = [exportId] } " +
                        "else { ctx._source.exports += exportId; ctx._source.exports = ctx._source.exports.unique(); }",
                "params", ImmutableMap.of("exportId", "exportId"));

        client.preparePutIndexedScript()
                .setScriptLang("groovy")
                .setId("asset_append_export")
                .setSource(script1)
                .get();

        Map<String, Object> script2 = ImmutableMap.of(
                "script", "if (ctx._source.folders == null ) { ctx._source.folders = [folderId] } else " +
                        "{ ctx._source.folders += folderId; ctx._source.folders = ctx._source.folders.unique(); }",
                "params", ImmutableMap.of("folderId", "folderId"));

        client.preparePutIndexedScript()
                .setScriptLang("groovy")
                .setId("asset_append_folder")
                .setSource(script2)
                .get();

        Map<String, Object> script3 = ImmutableMap.of(
                "script", "if (ctx._source.folders != null ) { ctx._source.folders.removeIf( {f -> f == folderId} )}",
                "params", ImmutableMap.of("folderId", "folderId"));

        client.preparePutIndexedScript()
                .setScriptLang("groovy")
                .setId("asset_remove_folder")
                .setSource(script3)
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
        Resource mapping = getLatestMappingVersion("classpath:/db/mappings/*.json");
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
}
