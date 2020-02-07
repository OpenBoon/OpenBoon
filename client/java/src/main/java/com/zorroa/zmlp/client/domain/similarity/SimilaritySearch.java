package com.zorroa.zmlp.client.domain.similarity;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;

import java.io.IOException;
import java.util.*;

public class SimilaritySearch extends TermQueryBuilder {

    List<Map> batch;

    public SimilaritySearch(String fieldName) {
        super(fieldName, new ArrayList());
        batch = (List)value;
    }

    public SimilaritySearch add(String hash, Double weight){
        Map<String, Object> map = new HashMap();
        map.put("hash", hash);
        map.put("weight", weight);
        batch.add(map);
        return this;
    }

    public SimilaritySearch add(String hash){
        Map<String, Object> map = new HashMap();
        map.put("hash", hash);
        map.put("weight", 1.0);
        batch.add(map);
        return this;
    }

    public SimilaritySearch addAll(List<Map<String, Object>> mapList){
        batch.addAll(mapList);
        return this;
    }


    @Override
    public String getWriteableName() {
        return "similarity";
    }

    @Override
    protected void doXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(this.getName());

        builder.field(this.fieldName, this.value.toString());
        this.printBoostAndQueryName(builder);
        builder.endObject();
    }
}
