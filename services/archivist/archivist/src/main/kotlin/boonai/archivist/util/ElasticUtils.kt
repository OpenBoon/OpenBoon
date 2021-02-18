package boonai.archivist.util

import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.DeprecationHandler
import org.elasticsearch.common.xcontent.NamedXContentRegistry
import org.elasticsearch.common.xcontent.XContentFactory
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.search.SearchModule
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

/**
 * ElasticSearch utility functions.
 */
object ElasticUtils {

    val searchModule = SearchModule(Settings.EMPTY, false, emptyList())
    val xContentRegistry = NamedXContentRegistry(searchModule.namedXContents)

    /**
     * Parse the given ES query string and return a QueryBuilder.
     */
    fun parse(query: String): QueryBuilder {
        val parser = XContentFactory.xContent(XContentType.JSON).createParser(
            xContentRegistry, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, query
        )

        val ssb = SearchSourceBuilder.fromXContent(parser)
        return ssb.query()
    }
}

/**
 * The ElasticSearchErrorTranslator takes error messages from ElasticSearch
 * and sanitizes them so they don't reveal information that could be
 * a security concern. This is specifically required for various security
 * audits.
 */
object ElasticSearchErrorTranslator {

    val logger: Logger = LoggerFactory.getLogger(ElasticSearchErrorTranslator::class.java)

    fun translate(message: String): String {
        val errorId = randomString(24)
        logger.warn("ElasticSearch Error '$errorId' $message")

        if (ALREADY_EXISTS_MESSAGE in message) {
            return "asset already exists"
        } else {
            for (pattern in RECOVERABLE_BULK_ERRORS) {
                val matcher = pattern.matcher(message)

                if (matcher.find()) {
                    val field = matcher.group(1)
                    return "field '$field' is not allowed, the wrong data type, or format"
                }
            }
        }

        return "Untranslatable asset error, reference id ='$errorId'"
    }

    private val RECOVERABLE_BULK_ERRORS = arrayOf(
        Pattern.compile("reason=failed to parse \\[(.*?)\\]"),
        Pattern.compile("\"term in field=\"(.*?)\"\""),
        Pattern.compile("mapper \\[(.*?)\\] of different type"),
        Pattern.compile("dynamic introduction of \\[(.*?)\\] within")
    )

    /**
     * The Exception message when a document already exists.
     */
    const val ALREADY_EXISTS_MESSAGE = "document already exists"
}
