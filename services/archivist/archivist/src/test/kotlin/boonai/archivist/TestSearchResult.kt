package boonai.archivist

/**
 * Created by chambers on 12/8/15.
 */
class TestSearchResult {

    var took: Int = 0
    var isTimed_out: Boolean = false
    var _shards: Shards? = null
    var hits: SearchHits? = null

    class Shards {
        var total: Int = 0
        var successful: Int = 0
        var failed: Int = 0
    }

    class SearchHits {
        var total: Int = 0
        private val max_score: Double = 0.toDouble()
    }
}
