package com.zorroa.zmlp.sdk;

import com.zorroa.zmlp.sdk.app.AssetApp;
import com.zorroa.zmlp.sdk.app.DataSourceApp;
import com.zorroa.zmlp.sdk.app.ProjectApp;

import java.io.IOException;
import java.util.Optional;

public class ZmlpApp {

    public final ZmlpClient zmlpClient;
    public final AssetApp assets;
    public final DataSourceApp datasources;
    public final ProjectApp projects;

    /**
     * Exposes the main PixelML API.
     *
     * @param apikey
     * @param server
     */
    public ZmlpApp(Object apikey, String server) {
        this.zmlpClient = new ZmlpClient(apikey, server);
        this.assets = new AssetApp(zmlpClient);
        this.datasources = new DataSourceApp(zmlpClient);
        this.projects = new ProjectApp(zmlpClient);
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

    public static ZmlpApp fromEnv() {

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

        return new ZmlpApp(apiKey, System.getenv("ZMLP_SERVER"));
    }
}
