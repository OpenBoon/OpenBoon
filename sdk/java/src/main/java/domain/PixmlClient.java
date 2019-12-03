package domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Map;

/*
 * PixmlClient is used to communicate to a Pixml API server.
 * */
public class PixmlClient {

    ObjectMapper mapper = new ObjectMapper();
    JsonNode apiKey;
    String server;

    public PixmlClient(Object apiKey, String server) {
        /*
        Create a new PixmlClient instance.

        Args:
            apikey: An API key in any supported form. (dict, base64 string, or open file handle)
            server: The url of the server to connect to. Defaults to https://api.Pixml.zorroa.com
            project_id: An optional project UUID for API keys with access to multiple projects.
            max_retries: Maximum number of retries to make if the API
                server is down, 0 for unlimited.
        */

        this.apiKey = loadApiKey(apiKey);
        this.server = server;
    }

    private JsonNode loadApiKey(Object apiKey) {

        if (apiKey instanceof Map) {
            return mapper.valueToTree(apiKey);
        } else if (apiKey instanceof String) {
            String apiKeyString = (String) apiKey;

            //Check if is a Base64 String
            Base64.getDecoder().decode(apiKeyString);
            //Check if is a valid JSON
            try {
                return mapper.readTree(apiKeyString);
            } catch (JsonProcessingException e) {
                System.out.println("Invalid Json Format");
            }
        }

        return null;
    }
}
