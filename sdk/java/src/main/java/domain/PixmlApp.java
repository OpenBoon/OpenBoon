package domain;

import java.io.IOException;
import java.util.Optional;

public class PixmlApp {

    private PixmlClient pixmlClient;
    private AssetApp assetApp;
    private DataSourceApp dataSourceApp;

    /**
     * Exposes the main PixelML API.
     *
     * @param apikey
     * @param server
     */
    public PixmlApp(Object apikey, String server) {

        String envServer = System.getenv("PIXML_SERVER");

        this.pixmlClient = new PixmlClient(apikey, Optional.ofNullable(server).orElse(envServer), null);
        this.assetApp = new AssetApp();
        this.dataSourceApp = new DataSourceApp(this);
    }

    public PixmlApp(Object apikey) {
        this(apikey, null);
    }

    /**
     * Env Variables Constructor
     * Create a PixmlApp configured via environment variables. This method
     * will not throw if the environment is configured improperly, however
     * attempting the use the PixmlApp instance to make a request
     * will fail.
     *
     * Environment Variables
     *  - PIXML_APIKEY : A base64 encoded API key.
     *  - PIXML_APIKEY_FILE : A path to a JSON formatted API key.
     *  - PIXML_SERVER : The URL to the Pixml API server.
     */

    public PixmlApp() {

        Optional<String> envApiKey = Optional.ofNullable(System.getenv("PIXML_APIKEY"));//base64
        Optional<String> envApiKeyFilePath = Optional.ofNullable(System.getenv("PIXML_APIKEY_FILE"));//file path

        String apiKey = null;

        if (envApiKey.isPresent()) {
            apiKey = envApiKey.get();
        } else if (envApiKeyFilePath.isPresent()) {
            apiKey = envApiKeyFilePath.map((String filePath) -> {
                try {
                    return Utils.readTextFromFile(filePath);
                } catch (IOException e) {
                    System.out.println("Invalid Key File Path.");
                    return null;
                }
            }).get();
        }


        // load Variables
        this.pixmlClient = new PixmlClient(apiKey, System.getenv("PIXML_SERVER"), null);
        this.assetApp = new AssetApp();
        this.dataSourceApp = new DataSourceApp(this);
    }

    public PixmlClient getPixmlClient() {
        return pixmlClient;
    }
    public void setPixmlClient(PixmlClient pixmlClient) {
        this.pixmlClient = pixmlClient;
    }

    public AssetApp getAssetApp() {
        return assetApp;
    }

    public void setAssetApp(AssetApp assetApp) {
        this.assetApp = assetApp;
    }

    public DataSourceApp getDataSourceApp() {
        return dataSourceApp;
    }

    public void setDataSourceApp(DataSourceApp dataSourceApp) {
        this.dataSourceApp = dataSourceApp;
    }
}
