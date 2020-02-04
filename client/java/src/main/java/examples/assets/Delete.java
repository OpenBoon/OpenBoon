package examples.assets;

import com.zorroa.zmlp.client.ZmlpApp;
import com.zorroa.zmlp.client.domain.asset.Asset;

import java.util.Map;

public class Delete {
    public static void main(String[] args) {

        // Initialize ZmlpApp
        ZmlpApp zmlpApp = new ZmlpApp("PIXML-APIKEY", "Server URL or Null for Default");

        // Asset to be deleted
        Asset asset = zmlpApp.assets.getById("abc123");

        // Elastic Search return value
        Map deleteES = zmlpApp.assets.delete(asset);

        /**
         * Example of Return Value
         * https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete.html
         *
         *{
         *     "_shards" : {
         *         "total" : 2,
         *         "failed" : 0,
         *         "successful" : 2
         *     },
         *     "_index" : "twitter",
         *     "_type" : "_doc",
         *     "_id" : "1",
         *     "_version" : 2,
         *     "_primary_term": 1,
         *     "_seq_no": 5,
         *     "result": "deleted"
         * }
         *
         */
    }
}
