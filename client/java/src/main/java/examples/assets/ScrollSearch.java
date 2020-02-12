package examples.assets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.zorroa.zmlp.client.Json;
import com.zorroa.zmlp.client.ZmlpApp;
import com.zorroa.zmlp.client.domain.asset.Asset;
import com.zorroa.zmlp.client.domain.asset.AssetSearchScroller;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.List;
import java.util.Map;

public class ScrollSearch {
    public static void main(String[] args) throws JsonProcessingException {

        // Initialize ZmlpApp
        ZmlpApp zmlpApp = new ZmlpApp("PIXML-APIKEY", "Server URL or Null for Default");

        String query = "{\"query\": {\"term\": {\"source.filename\": \"dog.jpg\"}}}";
        Map elementQueryTerms = Json.mapper.readValue(query, Map.class);

        // Or use SearchSourceBuilder
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(QueryBuilders.termQuery("source.filename","dog.jpg"));


        // Timeout of Scroll Timeout
        //AssetSearchScroller searchScroller = zmlpApp.assets.scrollSearch(elementQueryTerms, "1m");
        AssetSearchScroller searchScroller = zmlpApp.assets.scrollSearch(searchSourceBuilder, "1m");

        // List of Paged Assets
        List<Asset> list = searchScroller.next();
    }
}
