package boonai.similarity

import org.apache.lucene.index.LeafReaderContext
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.index.fielddata.ScriptDocValues
import org.elasticsearch.plugins.Plugin
import org.elasticsearch.plugins.ScriptPlugin
import org.elasticsearch.script.ScoreScript
import org.elasticsearch.script.ScriptContext
import org.elasticsearch.script.ScriptEngine
import org.elasticsearch.search.lookup.SearchLookup
import java.util.logging.Logger

class SimilarityPlugin : Plugin(), ScriptPlugin {

    override fun getScriptEngine(settings: Settings?, ctx: Collection<ScriptContext<*>>?): ScriptEngine {
        return SimilarityEngine()
    }

    private class SimilarityFactory : ScoreScript.Factory {
        override fun newFactory(params: MutableMap<String, Any>, lookup: SearchLookup): ScoreScript.LeafFactory {
            return SimilarityLeafFactory(params, lookup)
        }
    }

    private class SimilarityEngine : ScriptEngine {

        override fun getSupportedContexts(): Set<ScriptContext<*>> {
            return setOf(ScoreScript.CONTEXT)
        }

        override fun getType(): String {
            return "zorroa-similarity"
        }

        override fun <T> compile(
            name: String,
            code: String,
            context: ScriptContext<T>,
            params: Map<String, String>
        ): T {

            if ("similarity" == code) {
                return context.factoryClazz.cast(SimilarityFactory()) as T
            }

            throw IllegalArgumentException("Unknown script name $code")
        }
    }

    private class SimilarityLeafFactory constructor(
        val params: MutableMap<String, Any>,
        val lookup: SearchLookup
    ) : ScoreScript.LeafFactory {

        private val field: String = params.getValue("field") as String
        private val charHashes: List<String>
        private val minScore: Double = params.getOrDefault("minScore", 0.75) as Double
        private val resolution: Int = params.getOrDefault("resolution", 16) as Int

        init {

            if (params["hashes"] == null) {
                throw IllegalArgumentException("Hashes cannot be null")
            }

            charHashes = (params["hashes"] as List<String>)
                .mapNotNull { it }
                .filter { it.isNotEmpty() }
        }

        override fun needs_score(): Boolean {
            return true
        }

        override fun newInstance(ctx: LeafReaderContext?): ScoreScript {

            return object : ScoreScript(params, lookup, ctx) {

                val simEngine = SimilarityEngine(
                    charHashes,
                    minScore,
                    resolution
                )

                override fun execute(explanationHolder: ExplanationHolder?): Double {

                    val strings: ScriptDocValues.Strings
                    try {
                        if (doc[field]?.size ?: 0 > 0) {
                            strings = doc[field] as ScriptDocValues.Strings
                        } else {
                            return noScore
                        }
                    } catch (e: Exception) {
                        return noScore
                    }

                    if (charHashes.isEmpty()) {
                        return noScore
                    }

                    val score = simEngine.execute(strings)
                    return if (score >= minScore) score else noScore
                }
            }
        }
    }

    companion object {

        const val noScore = 0.0

        private val logger = Logger.getLogger("similarity-plugin")
    }
}
