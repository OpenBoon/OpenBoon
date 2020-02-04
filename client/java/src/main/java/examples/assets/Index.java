package examples.assets;

import com.zorroa.zmlp.client.ZmlpApp;
import com.zorroa.zmlp.client.domain.asset.Asset;

import java.util.Map;

public class Index {

    public static void main(String[] args) {
        // Initialize ZmlpApp
        ZmlpApp zmlpApp = new ZmlpApp("PIXML-APIKEY", "Server URL or Null for Default");

        // Get an Asset
        Asset asset = zmlpApp.assets.getById("dd0KZtqyec48n1q1ffogVMV5yzthRRGx");

        // Modify asset's attributes
        asset.setAttr("aux.my_field",1000);
        asset.removeAttr("aux.other_field");
        Map returnValue = zmlpApp.assets.index(asset);

        /**
         * Example return value:
         *   {
         *     "_index" : "v4mtygyqqpsjlcnv",
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
