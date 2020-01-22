package examples.assets;

import com.zorroa.zmlp.client.ZmlpApp;
import com.zorroa.zmlp.client.domain.asset.Asset;

public class GetAssetById {

    public static void main(String[] args) {

        // Initialize ZmlpApp
        ZmlpApp zmlpApp = new ZmlpApp("PIXML-APIKEY", "Server URL or Null for Default");

        // Get Asset by it's Id
        Asset asset = zmlpApp.assets.getById("dd0KZtqyec48n1q1ffogVMV5yzthRRGx");

    }
}
