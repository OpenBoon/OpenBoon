package com.zorroa.archivist;

/**
 * Created by chambers on 12/8/15.
 */
public class TestSearchResult {

    private int took;
    private boolean timed_out;
    private Shards _shards;
    private SearchHits hits;

    public int getTook() {
        return took;
    }

    public void setTook(int took) {
        this.took = took;
    }

    public boolean isTimed_out() {
        return timed_out;
    }

    public void setTimed_out(boolean timed_out) {
        this.timed_out = timed_out;
    }

    public Shards get_shards() {
        return _shards;
    }

    public void set_shards(Shards _shards) {
        this._shards = _shards;
    }

    public SearchHits getHits() {
        return hits;
    }

    public void setHits(SearchHits hits) {
        this.hits = hits;
    }

    public static class Shards {
        private int total;
        private int successful;
        private int failed;

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getSuccessful() {
            return successful;
        }

        public void setSuccessful(int successful) {
            this.successful = successful;
        }

        public int getFailed() {
            return failed;
        }

        public void setFailed(int failed) {
            this.failed = failed;
        }
    }

    public static class SearchHits {
        private int total;
        private double max_score;

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }
    }
}
