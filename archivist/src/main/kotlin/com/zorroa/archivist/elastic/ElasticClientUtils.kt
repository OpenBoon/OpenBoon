package com.zorroa.archivist.elastic

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Splitter
import com.google.common.collect.ImmutableMap
import org.elasticsearch.client.Client
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import java.io.IOException
import java.util.regex.Pattern

/**
 * Created by chambers on 4/15/16.
 */
object ElasticClientUtils {

    private val logger = LoggerFactory.getLogger(ElasticClientUtils::class.java)

    private val REMOVE_SCRIPT = "if (ctx._source.zorroa == null) { return; }; " +
            "if (ctx._source.zorroa['links'] == null) { return; }; " +
            "if (ctx._source.zorroa['links'][type] == null) { return; }; " +
            "ctx._source.zorroa['links'][type].removeIf({i-> i==id});"

    private val APPEND_SCRIPT = "if (ctx._source.zorroa == null) { ctx._source.zorroa = [:]; };  " +
            "if (ctx._source.zorroa['links'] == null) { ctx._source.zorroa['links'] = [:]; };  " +
            "if (ctx._source.zorroa['links'][type] == null) { ctx._source.zorroa['links'][type] = []; };" +
            "ctx._source.zorroa['links'][type] += id; " +
            "ctx._source.zorroa['links'][type] = ctx._source.zorroa['links'][type].unique();"

    private val MAPPING_NAMING_CONV = Pattern.compile("^V(\\d+)__(.*?).json$")


    @Throws(IOException::class)
    fun createEventLogTemplate(client: Client) {
        try {
            client.admin().indices()
                    .prepareDeleteTemplate("eventlog").get()
        } catch (e: Exception) {
            logger.warn("Did not delete old eventlog template")
        }

    }

    fun createIndexedScripts(client: Client) {
        createRemoveLinkScript(client)
        createAppendLinkScript(client)
    }

    fun createRemoveLinkScript(client: Client) {
        val script = ImmutableMap.of<String, Any>(
                "script", REMOVE_SCRIPT,
                "params", ImmutableMap.of("type", "type", "id", "id"))

        /*
        client.preparePutIndexedScript()
                .setScriptLang("groovy")
                .setId("remove_link")
                .setSource(script)
                .get()
                */
    }

    fun createAppendLinkScript(client: Client) {
        val script = ImmutableMap.of<String, Any>(
                "script", APPEND_SCRIPT,
                "params", ImmutableMap.of("type", "type", "id", "id"))

        /*
        client.preparePutIndexedScript()
                .setScriptLang("groovy")
                .setId("append_link")
                .setSource(script)
                .get()
                */
    }


    fun deleteAllIndexes(client: Client) {
        client.admin().indices().prepareDelete("_all").get()
    }


    @JvmOverloads
    fun refreshIndex(client: Client, sleep: Long = 10) {
        try {
            Thread.sleep(sleep / 2)
        } catch (e: InterruptedException) {
        }

        client.admin().indices().prepareRefresh("_all").get()
        try {
            Thread.sleep(sleep / 2)
        } catch (e: InterruptedException) {
        }

    }


    @Throws(IOException::class)
    fun createLatestMapping(client: Client, index: String, searchPath: String) {
        /*
        val mapping = getLatestMappingVersion(searchPath)
        client.admin()
                .indices()
                .prepareCreate(index)
                .setSource(ByteStreams.toByteArray(mapping!!.inputStream))
                .get()
                */
    }

    @Throws(IOException::class)
    fun createLatestMapping(client: Client, index: String) {

        //val mapping = getLatestMappingVersion("classpath:/db/mappings/$index/*.json")
        //client.admin()
         //       .indices()
          //      .prepareCreate(index)
           //     .setSource(ByteStreams.toByteArray(mapping!!.inputStream))
             //   .get()

    }

    @Throws(IOException::class)
    fun getLatestMappingVersion(searchPath: String): Resource? {

        val resolver = PathMatchingResourcePatternResolver(
                ElasticClientUtils::class.java.classLoader)
        val resources = resolver.getResources(searchPath)

        var latestVersion: Resource? = null
        var latestVerionNumber = 0

        for (resource in resources) {
            val matcher = MAPPING_NAMING_CONV.matcher(resource.filename)
            if (!matcher.matches()) {
                logger.warn("'{}' is not using the proper naming convention.", resource.filename)
                continue
            }

            val version = Integer.valueOf(matcher.group(1))
            if (version > latestVerionNumber) {
                latestVerionNumber = version
                latestVersion = resource
            }
        }
        logger.info("Loading latest elastic version: {}", latestVerionNumber)
        return latestVersion
    }

    fun getFieldType(client: Client, index: String, type: String, field: String): String {
        val cs = client.admin().cluster().prepareState().execute().actionGet().state
        val imd = cs.metaData.aliasAndIndexLookup[index]!!.getIndices()[0]
        val mmd = imd.mapping(type)
        val source = mmd.source()
        try {

            val mappingNode = ObjectMapper().readTree(source.uncompressed())
            var propertiesNode: JsonNode? = mappingNode.get("asset")

            for (f in Splitter.on('.').split(field)) {
                propertiesNode = propertiesNode!!.get("properties").get(f)
            }

            if (propertiesNode != null) {
                return propertiesNode.get("type").asText()
            }

        } catch (e: Exception) {
            logger.warn("Failed to determine type of field: {}, assuming string.", field)
        }

        return "string"
    }

    fun getMapping(client: Client, alias: String, type: String): Map<String, Any> {
        val cs = client.admin().cluster().prepareState().setIndices(
                alias).execute().actionGet().state
        /*
        for (index in cs.metaData.concreteAllOpenIndices()) {
            val imd = cs.metaData.index(index)
            val mdd = imd.mapping(type)
            try {
                return mdd.sourceAsMap
            } catch (e: IOException) {
                throw ArchivistException(e)
            }

        }
        */
        return ImmutableMap.of()
    }
}
