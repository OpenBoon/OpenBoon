package examples.assets;

import com.zorroa.zmlp.client.ZmlpApp;
import com.zorroa.zmlp.client.domain.asset.Asset;

import java.util.HashMap;
import java.util.Map;

public class AssetUpdate {
    public static void main(String[] args) {

        // Initialize ZmlpApp
        ZmlpApp zmlpApp = new ZmlpApp("PIXML-APIKEY", "Server URL or Null for Default");

        // Get an Asset by Id
        Asset asset = zmlpApp.assets.getById("abc123");

        // Initialize a new Document for partial update
        Map<String, Object> newDocument = new HashMap();
        Map<String, Object> aux = new HashMap();
        aux.put("captain", "kirk");
        newDocument.put("aux", aux);

        // Update Asset returns an Elastic Search Object
        Map elObject = zmlpApp.assets.update(asset.getId(), newDocument);

        /**
         * Example return value:
         *   {
         *     "_index" : "9l0l2skwmuesufff",
         *     "_type" : "_doc",
         *     "_id" : "dd0KZtqyec48n1q1fniqVMV5yllhRRGx",
         *     "_version" : 2,
         *     "result" : "updated",
         *     "_shards" : {
         *       "total" : 1,
         *       "successful" : 1,
         *       "failed" : 0
         *     },
         *     "_seq_no" : 1,
         *     "_primary_term" : 1
         *   }
         */

    }
}
