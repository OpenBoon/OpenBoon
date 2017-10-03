package com.zorroa.common.elastic;

import org.apache.lucene.search.Explanation;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.SearchShardTarget;
import org.elasticsearch.search.highlight.HighlightField;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class SingleHit implements SearchHit {

    private final GetResponse rsp;
    public SingleHit(GetResponse rsp) {
        this.rsp = rsp;
    }

    @Override
    public float score() {
        return 0;
    }

    @Override
    public float getScore() {
        return 0;
    }

    @Override
    public String index() {
        return rsp.getIndex();
    }

    @Override
    public String getIndex() {
        return rsp.getIndex();
    }

    @Override
    public String id() {
        return rsp.getId();
    }

    @Override
    public String getId() {
        return rsp.getId();
    }

    @Override
    public String type() {
        return rsp.getType();
    }

    @Override
    public String getType() {
        return rsp.getType();
    }

    @Override
    public NestedIdentity getNestedIdentity() {
        return null;
    }

    @Override
    public long version() {
        return rsp.getVersion();
    }

    @Override
    public long getVersion() {
        return rsp.getVersion();
    }

    @Override
    public BytesReference sourceRef() {
        return null;
    }

    @Override
    public BytesReference getSourceRef() {
        return null;
    }

    @Override
    public byte[] source() {
        return rsp.getSourceAsBytes();
    }

    @Override
    public boolean isSourceEmpty() {
        return rsp.getSource().isEmpty();
    }

    @Override
    public Map<String, Object> getSource() {
        return rsp.getSource();
    }

    @Override
    public String sourceAsString() {
        return rsp.getSourceAsString();
    }

    @Override
    public String getSourceAsString() {
        return rsp.getSourceAsString();
    }

    @Override
    public Map<String, Object> sourceAsMap() throws ElasticsearchParseException {
        return rsp.getSourceAsMap();
    }

    @Override
    public Explanation explanation() {
        return null;
    }

    @Override
    public Explanation getExplanation() {
        return null;
    }

    @Override
    public SearchHitField field(String fieldName) {
        return new SingleHitField(rsp.getField(fieldName));
    }

    @Override
    public Map<String, SearchHitField> fields() {
        return null;
    }

    @Override
    public Map<String, SearchHitField> getFields() {
        return null;
    }

    @Override
    public Map<String, HighlightField> highlightFields() {
        return null;
    }

    @Override
    public Map<String, HighlightField> getHighlightFields() {
        return null;
    }

    @Override
    public Object[] sortValues() {
        return new Object[0];
    }

    @Override
    public Object[] getSortValues() {
        return new Object[0];
    }

    @Override
    public String[] matchedQueries() {
        return new String[0];
    }

    @Override
    public String[] getMatchedQueries() {
        return new String[0];
    }

    @Override
    public SearchShardTarget shard() {
        return null;
    }

    @Override
    public SearchShardTarget getShard() {
        return null;
    }

    @Override
    public Map<String, SearchHits> getInnerHits() {
        return null;
    }

    @Override
    public Iterator<SearchHitField> iterator() {
        return null;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {

    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {

    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return null;
    }
}
