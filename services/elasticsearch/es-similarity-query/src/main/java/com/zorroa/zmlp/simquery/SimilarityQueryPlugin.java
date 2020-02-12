package com.zorroa.zmlp.simquery;

import java.util.Collections;
import java.util.List;
import org.apache.lucene.search.Query;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.SearchPlugin;

public class SimilarityQueryPlugin extends Plugin implements SearchPlugin {

    public List<QuerySpec<?>> getQueries() {
        return Collections.singletonList(
                new QuerySpec<>(SimilarityQueryBuilder.NAME, SimilarityQueryBuilder::new, SimilarityQueryBuilder::fromXContent));
    }

    public static class SimilarityQuery extends Query {

        @Override
        public String toString(String s) {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }
}
