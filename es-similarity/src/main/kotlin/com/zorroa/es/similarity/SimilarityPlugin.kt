package com.zorroa.es.similarity

import org.apache.lucene.index.LeafReaderContext
import org.elasticsearch.index.fielddata.ScriptDocValues
import org.elasticsearch.plugins.Plugin
import org.elasticsearch.script.ScriptContext
import org.elasticsearch.script.ScriptEngine
import org.elasticsearch.script.SearchScript
import org.elasticsearch.search.lookup.SearchLookup
import java.util.*

/**
 * Created by chambers on 12/6/16.
 */

private const val NORM = 100.0

private const val NO_SCORE = 0.0

class SimilarityPlugin: Plugin(), ScriptEngine {

    override fun <T : Any?> compile(scriptName: String, source: String, ctx: ScriptContext<T>, params: MutableMap<String, String>): T {

        if ("similarity" != source) {
            throw IllegalArgumentException("Invalid script source $source")
        }

        val factory = { args: Map<String, String>, lookup: SearchLookup ->
            object : SearchScript.LeafFactory {

                private val field: String = params["field"] as String
                private val charHashes: MutableList<String>
                private val weights: MutableList<Float>
                private var length = 0
                private val minScore: Double =
                        params.getOrDefault("minScore", "1").toDouble() / NORM

                private var resolution: Int = 0
                private val header: Boolean
                private var version: Char
                private val dataPos: Int
                private var numHashes: Int = 0
                private var singleScore: Double
                private var possibleScore: Double = 0.toDouble()

                init {

                    resolution = 15

                    val _hashes = params["hashes"] as List<String>
                    var _weights: List<Float>? = params["weights"] as List<Float>

                    if (_hashes == null) {
                        throw IllegalArgumentException(
                                "Hashes cannot be null")
                    }

                    if (_weights == null) {
                        _weights = Collections.nCopies(_hashes.size, 1.0f)
                    }

                    if (_hashes.size != _weights!!.size) {
                        throw IllegalArgumentException(
                                "HammingDistanceScript weights must align with hashes")
                    }

                    /**
                     * Go through all the values and remove the null
                     * values and populate the charHashes and
                     * weights fields with valid values.
                     */
                    charHashes = mutableListOf()
                    weights = mutableListOf()
                    for (i in _hashes.indices) {
                        val hash = _hashes[i]
                        if (hash == null || hash.isEmpty()) {
                            continue
                        }
                        charHashes.add(hash)
                        weights.add(_weights[i])
                    }

                    /**
                     * If there are no valid hashes left, initialize to defaults
                     */
                    if (charHashes.isEmpty()) {
                        singleScore = 0.0
                        numHashes = 0
                        dataPos = 0
                        version = '\u0000'
                        header = false
                    } else {
                        /**
                         * Use the first hash to determine if there is a header.
                         */
                        val hash = charHashes[0]
                        header = hash[0] == '#'
                        length = hash.length
                        numHashes = charHashes.size

                        /**
                         * TODO: more sophisticated header parsing.
                         *
                         * There are 2 fields every has leads with:
                         * 1 char: version
                         * 2 chars: position of data (called "headerSize" here)
                         *
                         * A version 0 hash has 1 field, resolution.
                         */
                        if (header) {
                            version = hash[1]

                            // The start position of the data.
                            dataPos = Integer.parseInt(hash.substring(2, 4), 16)

                            if (version.toInt() <= 0) {
                                // Resolution is the next byte.
                                resolution = Integer.parseInt(hash.substring(4, 6), 16)
                            }
                        } else {
                            version = 0.toChar()
                            dataPos = 0
                        }

                        // To get the proper score, we subtract header size from the length here.
                        singleScore = (resolution * (length - dataPos)).toDouble()
                        possibleScore = singleScore * numHashes
                    }
                }

                override fun needs_score(): Boolean {
                    return false
                }

                override fun newInstance(ctx: LeafReaderContext): SearchScript {
                    return object : SearchScript (args, lookup, ctx) {

                        override fun runAsDouble(): Double {
                            val strings: ScriptDocValues.Strings

                            if (singleScore == 0.0) {
                                return NO_SCORE
                            }

                            if (doc.containsKey(field)) {
                                strings = doc[field] as ScriptDocValues.Strings
                            } else {
                                return NO_SCORE
                            }

                            val values = strings.values
                            if (values.size == 0) {
                                return NO_SCORE
                            }
                            val score = charHashesComparison(strings.values)
                            return if (score >= minScore) score else NO_SCORE
                        }


                        fun charHashesComparison(values: List<String>): Double {
                            var highestScore = 0.0

                            for (i in 0 until numHashes) {
                                for (fieldValue in values) {
                                    val ver = fieldValue.toByteArray()[1]
                                    val hash = charHashes[i] ?: continue
                                    if (header) {
                                        if (ver != hash[1].toByte()) {
                                            continue
                                        }
                                    } else {
                                        if (fieldValue.length != hash.length) {
                                            continue
                                        }
                                    }
                                    val score = weights[i] * hammingDistance(fieldValue, hash)
                                    if (score > highestScore) {
                                        highestScore = score
                                    }
                                }
                            }
                            return normalize(highestScore)
                        }

                        fun normalize(score: Double): Double {
                            var score = score
                            if (possibleScore == 0.0) {
                                return 0.0
                            }
                            score /= possibleScore
                            return score
                        }

                        fun hammingDistance(lhs: String, rhs: String): Double {
                            var score = singleScore
                            for (i in dataPos until length) {
                                score -= Math.abs(lhs[i] - rhs[i]).toDouble()
                            }
                            return score
                        }

                    }
                }
            }
        }

        return ctx.factoryClazz!!.cast(factory)
    }

    override fun close() { }

    override fun getType() : String {
        return "similarity"
    }
}
