package com.zorroa.es.kwconf

import java.util.logging.Logger
import org.apache.lucene.index.LeafReaderContext
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.plugins.Plugin
import org.elasticsearch.plugins.ScriptPlugin
import org.elasticsearch.script.ScoreScript
import org.elasticsearch.script.ScriptContext
import org.elasticsearch.script.ScriptEngine
import org.elasticsearch.search.lookup.SearchLookup

/**
 *
 * The Keyword Confidence plugin allows you to filter keyword/confidence pairs. The pairs
 * must be described as :
 *
 * [
 *     { "label": "dog", "score": 0.87 },
 *     { "label": "ardvark", score: 0.62 },
 *     { "label": "badger", score: 0.38 }
 * ]
 *
 */
class KwConfPlugin : Plugin(), ScriptPlugin {

    companion object {

        /**
         * The type of script. This must be specified as the language in your
         * ES scriptFunction query.
         */
        private const val scriptType = "zorroa-kwconf"

        /**
         * The script ID. This must be specified as the idOrCode in your
         * ES scriptFunction query.
         */
        private const val scriptId = "kwconf"

        private val logger = Logger.getLogger("zorroa-kw-conf")
    }

    /**
     * A factory class called by ES which returns an instance of a search script.  Each ES
     * thread will create its own instance.
     */
    private class KwConfLeafFactory constructor(
        val params: MutableMap<String, Any>,
        val lookup: SearchLookup
    ) : ScoreScript.LeafFactory {

        private val field: String = params["field"] as String
        private val keywords: Set<String> = (params["labels"] as List<String>?).orEmpty().toSet()
        private val range: List<Double> = (params["range"] as List<Double>?) ?: listOf(.75, 1.0)

        override fun newInstance(ctx: LeafReaderContext?): ScoreScript {

            /**
             * This object expression returns and anonymous subclass of SearchScript with
             * runAsDouble implemented.
             */
            return object : ScoreScript(params, lookup, ctx) {

                override fun execute(explanationHolder: ExplanationHolder?): Double {
                    var score = 0.0

                    try {
                        /**
                         * The value of our field must be a kwconf structure.  Just skip over assets
                         * where it does not exist or cannot be cast.
                         */
                        val kwconf = lookup.source().extractValue(field) ?: return score
                        val labels: List<Map<String, Any>> = kwconf as List<Map<String, Any>>
                        for (map in labels) {
                            val keyword = map.getValue("label").toString()
                            if (keyword in keywords) {
                                val conf = (map.getValue("score") as Number).toDouble()
                                if (isWithinRange(conf)) {
                                    score += conf
                                }
                            }
                        }
                    } catch (e: Exception) {
                        logger.warning("Error processing zorroa kw-conf search ${e.message}")
                    }

                    return score
                }

                private fun isWithinRange(conf: Double): Boolean {
                    return conf >= range[0] && conf <= range[1]
                }
            }
        }

        override fun needs_score(): Boolean = false
    }

    /*
     * Everything below here is boilerplate needed to interface with the ES plugin system.
     */

    override fun getScriptEngine(settings: Settings?, ctx: Collection<ScriptContext<*>>?): ScriptEngine {
        return KwConfEngine()
    }

    private class KwConfFactory : ScoreScript.Factory {
        override fun newFactory(params: MutableMap<String, Any>, lookup: SearchLookup): ScoreScript.LeafFactory {
            return KwConfLeafFactory(params, lookup)
        }
    }

    private class KwConfEngine : ScriptEngine {

        override fun getType(): String = scriptType

        override fun <T> compile(name: String, code: String, context: ScriptContext<T>, params: Map<String, String>): T {

            if (scriptId == code) {
                return context.factoryClazz.cast(KwConfFactory()) as T
            }

            throw IllegalArgumentException("Unknown script name $code")
        }

        override fun getSupportedContexts(): Set<ScriptContext<*>> {
            return setOf(ScoreScript.CONTEXT)
        }
    }
}
