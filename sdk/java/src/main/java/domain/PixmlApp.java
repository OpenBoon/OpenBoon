package domain;

/* Exposes the main PixelML API.*/

import java.util.Optional;

public class PixmlApp {

    PixmlClient pixmlClient;
    AssetApp assetApp;
    DataSourceApp dataSourceApp;

    public PixmlApp(Object apikey, String server) {

        String envServer = System.getenv("PIXML_SERVER");

        this.pixmlClient = new PixmlClient(apikey, Optional.ofNullable(server).orElse(envServer));
        this.assetApp = new AssetApp();
        this.dataSourceApp = new DataSourceApp();
    }
}
