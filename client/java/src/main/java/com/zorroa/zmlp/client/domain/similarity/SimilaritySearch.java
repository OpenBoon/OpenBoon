package com.zorroa.zmlp.client.domain.similarity;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;

import java.io.IOException;
import java.util.*;

/**
 * Similarity Search Query Builder
 */
public class SimilaritySearch extends TermQueryBuilder {

    /**
     * Contains all Hash and Weight tuples
     */
    private List<Map> batch;

    /**
     * Constructor receives a Field Name to a List of Hashes
     * @param fieldName Name of the Field
     */
    public SimilaritySearch(String fieldName) {
        super(fieldName, new ArrayList());
        batch = (List)value;
    }

    /**
     * Add a tuple of hash and weight to the query
     * @param hash Reference to the image
     * @param weight Reference weight
     * @return SimilaritySearch object
     */
    public SimilaritySearch add(String hash, Double weight){
        Map<String, Object> map = new HashMap();
        map.put("hash", hash);
        map.put("weight", weight);
        batch.add(map);
        return this;
    }

    /**
     * Add a Hash to the query and use the default value of the weight
     * @param hash Reference to the image
     * @return SimilaritySearch object
     */
    public SimilaritySearch add(String hash){
        Map<String, Object> map = new HashMap();
        map.put("hash", hash);
        map.put("weight", 1.0);
        batch.add(map);
        return this;
    }

    /**
     * Add a list of tuples that must contain hash and weight
     * @param mapList List of Tuples
     * @return
     */
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
