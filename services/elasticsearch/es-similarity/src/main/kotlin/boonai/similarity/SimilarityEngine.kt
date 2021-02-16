package boonai.similarity

import kotlin.math.abs

/**
 * SimilarityEngine handles comparing two strings and returns a normalized
 * hamming distance value.
 */
class SimilarityEngine(
    val queryHashes: List<String>,
    minScore: Double,
    resolution: Int
) {
    private val queryHashLength = queryHashes[0].length
    private val maxSingleScore = (resolution * queryHashLength)
    private val maxSingleScoreDouble = maxSingleScore.toDouble()
    private val minSingleScore = maxSingleScore * minScore

    /**
     * Compares multiple hashes stored on the Asset against
     * hashes supplied by a query.  Returns the highest score.
     */
    fun execute(assetHashes: List<String>): Double {
        var highScore = 0
        for (assetHash in assetHashes) {
            highScore = highScore.coerceAtLeast(charHashesComparison(assetHash))
        }
        return highScore / maxSingleScoreDouble
    }

    private fun charHashesComparison(fieldValue: String?): Int {
        if (fieldValue == null || fieldValue.isEmpty()) {
            return 0
        }

        var highScore = 0
        val bytes = fieldValue.toCharArray()
        for (hash in queryHashes) {
            if (bytes.size != hash.length) {
                continue
            }

            val elementScore = hammingDistance(bytes, hash)
            if (elementScore >= minSingleScore) {
                highScore = highScore.coerceAtLeast(elementScore)
            }
        }
        return highScore
    }

    private fun hammingDistance(lhs: CharArray, rhs: String): Int {
        var score = maxSingleScore
        for (i in 0 until queryHashLength) {
            score -= abs(lhs[i] - rhs[i])
        }
        return score
    }
}
