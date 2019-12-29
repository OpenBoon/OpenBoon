package com.zorroa.zmlp.sdk;

import com.zorroa.zmlp.sdk.app.AssetApp;
import com.zorroa.zmlp.sdk.app.DataSourceApp;

import java.io.IOException;
import java.util.Optional;

public class ZmlpApp {

    private ZmlpClient zmlpClient;
    private AssetApp assetApp;
    private DataSourceApp dataSourceApp;

    /**
     * Exposes the main PixelML API.
     *
     * @param apikey
     * @param server
     */
    public ZmlpApp(Object apikey, String server) {

        String envServer = System.getenv("ZMLP_SERVER");

        this.zmlpClient = new ZmlpClient(apikey, Optional.ofNullable(server).orElse(envServer), null);
        this.assetApp = new AssetApp();
        this.dataSourceApp = new DataSourceApp(this);
    }

    public ZmlpApp(Object apikey) {
        this(apikey, null);
    }

    /**
     * Env Variables Constructor
     * Create a ZmlpApp configured via environment variables. This method
     * will not throw if the environment is configured improperly, however
     * attempting the use the ZmlpApp instance to make a request
     * will fail.
     *
     * Environment Variables
     *  - ZMLP_APIKEY : A base64 encoded API key.
     *  - ZMLP_APIKEY_FILE : A path to a JSON formatted API key.
     *  - ZMLP_SERVER : The URL to the ZMLP API server.
     */

    public ZmlpApp() {

        Optional<String> envApiKey = Optional.ofNullable(System.getenv("ZMLP_APIKEY"));//base64
        Optional<String> envApiKeyFilePath = Optional.ofNullable(System.getenv("ZMLP_APIKEY_FILE"));//file path

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
        this.zmlpClient = new ZmlpClient(apiKey, System.getenv("ZMLP_SERVER"), null);
        this.assetApp = new AssetApp();
        this.dataSourceApp = new DataSourceApp(this);
    }

    public ZmlpClient getZmlpClient() {
        return zmlpClient;
    }

    public void setZmlpClient(ZmlpClient zmlpClient) {
        this.zmlpClient = zmlpClient;
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
