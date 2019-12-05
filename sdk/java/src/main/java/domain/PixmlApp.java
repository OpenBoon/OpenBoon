package domain;

/* Exposes the main PixelML API.*/

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    public PixmlApp(Object apikey) {
        this(apikey, null);
    }

    // Env Variables Constructor
    public PixmlApp appFromEnv() {

        /*
        """
        Create a PixmlApp configured via environment variables. This method
        will not throw if the environment is configured improperly, however
        attempting the use the PixmlApp instance to make a request
        will fail.

        - PIXML_APIKEY : A base64 encoded API key.
        - PIXML_APIKEY_FILE : A path to a JSON formatted API key.
        - PIXML_SERVER : The URL to the Pixml API server.

        Returns:
            PixmlClient : A configured PixmlClient

        """
         */

        Optional<String> envApiKey = Optional.ofNullable(System.getenv("PIXML_APIKEY"));//base64
        Optional<String> envApiKeyFilePath = Optional.ofNullable(System.getenv("PIXML_APIKEY_FILE"));//file path

        String apiKey = null;

        if (envApiKey.isPresent()) {
            apiKey = envApiKey.get();
        } else if (envApiKeyFilePath.isPresent()) {
            apiKey = envApiKeyFilePath.map((String filePath) -> {
                try {
                    return new String(Files.readAllBytes(Paths.get(filePath)));
                } catch (IOException e) {
                    System.out.println("Invalid Key File Path.");
                    return null;
                }
            }).get();
        }

        return new PixmlApp(apiKey, System.getenv("PIXML_SERVER"));
    }


}
