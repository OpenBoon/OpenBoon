package examples.assets;

import com.zorroa.zmlp.client.ZmlpApp;
import com.zorroa.zmlp.client.domain.asset.Asset;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AssetBatchUpdate {
    public static void main(String[] args) {

        // Initialize ZmlpApp
        ZmlpApp zmlpApp = new ZmlpApp("PIXML-APIKEY", "Server URL or Null for Default");

        // Assets to be updated
        Asset asset1 = zmlpApp.assets.getById("abc123");
        asset1.setAttr("attr.update", "1");
        Asset asset2 = zmlpApp.assets.getById("123abc");
        asset2.setAttr("update.attr", "2");

        // Wrap assets into a list
        List<Asset> assetList = Arrays.asList(asset1, asset2);

        // Batch Index returns an Elastic Search Object
        Map elasticSearchMap = zmlpApp.assets.batchIndex(assetList);

        /**
         * Example return value:
         *  {
         *    "took" : 11,
         *    "errors" : false,
         *    "items" : [ {
         *      "index" : {
         *        "_index" : "qjdjbpkvwg0sgusl",
         *        "_type" : "_doc",
         *        "_id" : "dd0KZtqyec48n1q1fniqVMV5yllhRRGx",
         *        "_version" : 2,
         *        "result" : "updated",
         *        "_shards" : {
         *          "total" : 1,
         *          "successful" : 1,
         *          "failed" : 0
         *        },
         *        "_seq_no" : 1,
         *        "_primary_term" : 1,
         *        "status" : 200
         *      }
         *    } ]
         *  }
         */
    }
}
