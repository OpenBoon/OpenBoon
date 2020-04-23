package com.zorroa.zmlp.similarity

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
import kotlin.math.abs

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
        private val length: Int
        private val minScore: Double = params.getOrDefault("minScore", 0.75) as Double
        private val resolution: Int = 16
        private val numHashes: Int
        private val singleScore: Double

        init {

            if (params["hashes"] == null) {
                throw IllegalArgumentException("Hashes cannot be null")
            }

            charHashes = (params["hashes"] as List<String>)
                .mapNotNull { it }
                .filter { it.isNotEmpty() }

            /**
             * If there are no valid hashes left, initialize to defaults
             */
            if (charHashes.isEmpty()) {
                singleScore = 0.0
                numHashes = 0
                length = 0
            } else {
                val hash = charHashes[0]
                length = hash.length
                numHashes = charHashes.size
                singleScore = (resolution * length).toDouble()
            }
        }

        override fun needs_score(): Boolean {
            return true
        }

        override fun newInstance(ctx: LeafReaderContext?): ScoreScript {

            return object : ScoreScript(params, lookup, ctx) {

                override fun execute(explanationHolder: ExplanationHolder?): Double {

                    val strings: ScriptDocValues.Strings

                    if (doc[field]?.size ?: 0 > 0) {
                        strings = doc[field] as ScriptDocValues.Strings
                    } else {
                        return noScore
                    }

                    val score = charHashesComparison(strings.value)
                    return if (score >= minScore) score else noScore
                }

                fun charHashesComparison(fieldValue: String?): Double {
                    var totalScore = 0.0

                    if (numHashes == 0) {
                        return noScore
                    }

                    if (fieldValue == null || fieldValue.isEmpty()) {
                        return noScore
                    }

                    val bytes = fieldValue.toCharArray()
                    for (i in 0 until numHashes) {
                        val hash = charHashes[i]
                        if (bytes.size != hash.length) {
                            continue
                        }
                        val elementScore = normalize(hammingDistance(bytes, hash))
                        if (elementScore < minScore) {
                            return noScore
                        } else {
                            totalScore += elementScore
                        }
                    }

                    return totalScore
                }

                fun normalize(score: Double): Double {
                    return score / singleScore
                }

                fun hammingDistance(lhs: CharArray, rhs: String): Double {
                    var score = singleScore
                    for (i in 0 until length) {
                        score -= abs(lhs[i] - rhs[i])
                    }
                    return score
                }
            }
        }
    }

    companion object {

        private const val normFactor = 100.0

        private const val noScore = 0.0

        private val logger = Logger.getLogger(SimilarityPlugin::class.java.canonicalName)
    }
}
