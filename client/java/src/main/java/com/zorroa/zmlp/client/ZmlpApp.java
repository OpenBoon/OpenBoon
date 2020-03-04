package com.zorroa.zmlp.client;

import com.zorroa.zmlp.client.app.AssetApp;
import com.zorroa.zmlp.client.app.DataSourceApp;
import com.zorroa.zmlp.client.app.ProjectApp;
import com.zorroa.zmlp.client.domain.exception.ZmlpAppException;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
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
    public ZmlpApp(ApiKey apikey, String server) {
        this.zmlpClient = new ZmlpClient(apikey, server);
        this.assets = new AssetApp(zmlpClient);
        this.datasources = new DataSourceApp(zmlpClient);
        this.projects = new ProjectApp(zmlpClient);
    }

    public ZmlpApp(String apikey, String server) {
        this(loadApiKey(apikey), server);
    }


    public ZmlpApp(File apikey, String server) {
        this(loadApiKey(apikey), server);
    }

    /**
     * Env Variables Constructor
     * Create a ZmlpApp configured via environment variables. This method
     * will not throw if the environment is configured improperly, however
     * attempting the use the ZmlpApp instance to make a request
     * will fail.
     * <p>
     * Environment Variables
     * - ZMLP_APIKEY : A base64 encoded API key.
     * - ZMLP_APIKEY_FILE : A path to a JSON formatted API key.
     * - ZMLP_SERVER : The URL to the ZMLP API server.
     */

    private static ApiKey loadApiKey(File fileKey) throws ZmlpAppException {
        try {
            return Json.mapper.readValue(fileKey, ApiKey.class);
        } catch (IOException e) {
            throw new ZmlpAppException("Unable to load ZpiKey file " + fileKey, e);
        }
    }

    private static ApiKey loadApiKey(String base64key) {
        byte[] decoded = Base64.getDecoder().decode(base64key);
        try {
            return Json.mapper.readValue(decoded, ApiKey.class);
        } catch (IOException e) {
            throw new ZmlpAppException("Unable to load Base64 key", e);
        }
    }

    public static ZmlpApp fromEnv() throws ZmlpAppException {

        Optional<String> envApiKey = Optional.ofNullable(System.getenv("ZMLP_APIKEY"));//base64
        Optional<String> envApiKeyFilePath = Optional.ofNullable(System.getenv("ZMLP_APIKEY_FILE"));//file path

        ApiKey apiKey;

        if (envApiKey.isPresent()) {
            apiKey = loadApiKey(envApiKey.get());
        } else if (envApiKeyFilePath.isPresent()) {
            apiKey = loadApiKey(new File(envApiKeyFilePath.get()));
        } else {
            throw new ZmlpAppException("Unable to load api key from envirionment");
        }

        return new ZmlpApp(apiKey, System.getenv("ZMLP_SERVER"));
    }
}
