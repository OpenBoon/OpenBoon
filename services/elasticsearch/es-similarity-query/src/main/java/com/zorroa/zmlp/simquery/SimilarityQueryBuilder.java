package com.zorroa.zmlp.simquery;

import java.io.IOException;
import org.apache.lucene.search.Query;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.QueryShardContext;

public class SimilarityQueryBuilder extends AbstractQueryBuilder<SimilarityQueryBuilder> {

    public static final String NAME = "similarity";

    SimilarityQueryBuilder() {

    }

    public SimilarityQueryBuilder(StreamInput in) throws IOException {
        super(in);
    }

    @Override
    protected void doWriteTo(StreamOutput streamOutput) throws IOException {

    }

    @Override
    protected void doXContent(XContentBuilder xContentBuilder, Params params) throws IOException {

    }

    @Override
    protected Query doToQuery(QueryShardContext queryShardContext) throws IOException {
        return null;
    }

    @Override
    protected boolean doEquals(SimilarityQueryBuilder similarityQueryBuilder) {
        return false;
    }

    @Override
    protected int doHashCode() {
        return 0;
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    public static SimilarityQueryBuilder fromXContent(XContentParser parser) throws IOException {
        XContentParser.Token token = parser.nextToken();
        assert token == XContentParser.Token.END_OBJECT;
        return new SimilarityQueryBuilder();
    }
}
