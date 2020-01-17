package examples.assets;

import com.zorroa.zmlp.client.ZmlpApp;
import com.zorroa.zmlp.client.domain.asset.Asset;
import examples.ZmplUtil;

import java.util.UUID;

public class GetAssetById {

    public static void main(String[] args) {

        // Initialize ZmlpApp
        ZmlpApp zmlpApp = ZmplUtil.createZmplApp(UUID.randomUUID(), "PIXML-APIKEY");

        // Get Asset by it's Id
        Asset asset = zmlpApp.assets.getById("dd0KZtqyec48n1q1ffogVMV5yzthRRGx");

    }
}
