package examples.assets;

import com.zorroa.zmlp.client.ZmlpApp;
import com.zorroa.zmlp.client.domain.asset.AssetSearchResult;
import com.zorroa.zmlp.client.domain.similarity.SimilaritySearch;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchElementBySimilarity {
    public static void main(String[] args) {

        // Initialize ZmlpApp
        ZmlpApp zmlpApp = new ZmlpApp("PIXML-APIKEY", "Server URL or Null for Default");

        //Create Search Builder for Elastic Search
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // Add a SimilarityFilter to SimilaritySearch Query
        searchSourceBuilder.query(
                new SimilaritySearch("field")
                        .add("HASH-HASH-HASH-HASH-HASH-HASH-HASH"));

        // Json for Unique Filter
        /*
        {
           "query":{
              "similarity":{
                 "field":"[{weight=1.0, hash=HASH-HASH-HASH-HASH-HASH-HASH-HASH}]",
                 "boost":1.0
              }
           }
        }
         */

        //Or Use BatchSimilarity Filter to add multiple SimilarityFilters
        searchSourceBuilder.query(
                new SimilaritySearch("batchField")
                        .add("HASH11111111111111111")
                        .add("HASH22222222222222222222", 2.0)
        );

        //Json for multiple filters
        /*
        {
           "query":{
              "similarity":{
                 "batchField":"[
                 {weight=1.0, hash=HASH11111111111111111},
                 {weight=2.0, hash=HASH22222222222222222222}
                 ]",
              }
           }
        }
         */

        // Or use your List<Map<String,Object>> eachMap must contain a "hash" and could contain a "weight" properties.

        List<Map<String, Object>> filters = new ArrayList();
        Map hash1 = new HashMap();
        hash1.put("hash", "HASH-NUMBER-ONE");
        hash1.put("weight", 3.8);
        filters.add(hash1);

        Map hash2 = new HashMap();
        hash2.put("hash", "HASH-NUMBER-TWO");
        hash2.put("weight", 1);
        filters.add(hash2);

        searchSourceBuilder.query(
                new SimilaritySearch("batchField")
                        .addAll(filters));

        /*
        {
           "query":{
              "similarity":{
                 "batchField":"[{weight=3.8, hash=HASH-NUMBER-ONE}, {weight=1, hash=HASH-NUMBER-TWO}]",
                 "boost":1.0
              }
           }
        }
         */

        // Search
        AssetSearchResult search = zmlpApp.assets.search(searchSourceBuilder);


    }
}
