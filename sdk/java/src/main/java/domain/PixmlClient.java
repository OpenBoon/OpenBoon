package domain;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class PixmlClient {

    ObjectMapper mapper = new ObjectMapper();
    JsonNode apiKey;
    String server;

    String projectId;
    Integer maxRetries;

    private final String DEFAULT_SERVER_URL = "https://api.pixelml.com";

    /**
     * PixmlClient is used to communicate to a Pixml API server.
     * Create a new PixmlClient instance.
     *
     * @param apiKey An API key in any supported form. (dict, base64 string, or open file handle)
     * @param server The url of the server to connect to. Defaults to https://api.Pixml.zorroa.com
     * @param args   Contains max_entries and/or project_id. Project_id contains An optional project UUID for API keys with access to multiple projects.
     */
    public PixmlClient(Object apiKey, String server, Map args) {

        args = Optional.ofNullable(args).orElse(new HashMap());

        this.apiKey = loadApiKey(apiKey);
        this.server = Optional.ofNullable(server).orElse(DEFAULT_SERVER_URL);
        this.maxRetries = (Integer) args.getOrDefault("max_retries", 3);
        this.projectId = (String) args.get("project_id");
    }

    public PixmlClient(Object apiKey) {
        this(apiKey, null, null);
    }


    private JsonNode loadApiKey(Object apiKey) {

        if (apiKey instanceof Map) {
            return mapper.valueToTree(apiKey);
        } else if (apiKey instanceof byte[] || apiKey instanceof String) {
            String apiKeyString;
            if (apiKey instanceof byte[])
                apiKeyString = new String(Base64.getDecoder().decode((byte[]) apiKey));
            else
                apiKeyString = (String) apiKey;

            //Check if is a valid JSON
            try {
                return mapper.readTree(apiKeyString);
            } catch (JsonProcessingException e) {
                System.out.println("Invalid Json Format");
            }
        }

        return null;
    }

    /**
     * Generate the return some request headers.
     * The content-type for the request. Defaults to 'application/json'
     *
     * @return Map: An http header struct.
     */

    public Map<String, String> headers() {
        return this.headers("application/json");
    }

    /**
     * Generate the return some request headers.
     *
     * @param contentType The content-type for the request.
     * @return Map: An http header struct.
     */

    public Map<String, String> headers(String contentType) {

        String authorization = null;
        authorization = "Bearer %s".format(signRequest());
        Map header = new HashMap<String, String>();
        header.put("Authorization", authorization);

        Optional.ofNullable(contentType).ifPresent(
                (type) -> header.put("Content-Type", type));

        return header;

    }

    private String signRequest() {

        if (this.apiKey == null) {
            throw new RuntimeException("Unable to make request, no ApiKey has been specified.");
        }

        JWTCreator.Builder claimBuilder = JWT.create();

        claimBuilder.withClaim("aud", this.server);
        claimBuilder.withClaim("exp", Instant.now().plus(60, ChronoUnit.SECONDS).toEpochMilli());
        claimBuilder.withClaim("id", this.apiKey.get("id").textValue());

        //if PIXML_TASK_ID exists
        Optional.ofNullable(System.getenv("PIXML_TASK_ID"))
                .ifPresent((String taskId) -> {
                    claimBuilder.withClaim("taskId", taskId);
                    claimBuilder.withClaim("PIXML_JOB_ID", System.getenv("PIXML_JOB_ID"));
                });

        // if projectId exists
        Optional.ofNullable(this.projectId).ifPresent((String projectId) -> {
            claimBuilder.withClaim("projectId", projectId);
        });


        Algorithm sharedKey = Algorithm.HMAC512(this.apiKey.get("sharedKey").asText());
        String jwtEncoded = claimBuilder.sign(sharedKey);
        return jwtEncoded;
    }

    /**
     * Performs a post request.
     *
     * @param path An archivist URI path.
     * @param body The request body which will be serialized to json.
     * @return The http response object or an object deserialized from the response json if the ``json`` argument is true.
     * @throws IOException          An error occurred making the request or parsing the JSON response
     * @throws InterruptedException
     */
    public Map post(String path, Map body) throws IOException, InterruptedException {

        return this.makeRequest("post", path, body);
    }

    private Map makeRequest(String httpMethod, String path, Map body) throws InterruptedException, IOException {

        String httpResponseString = null;
        String url = getUrl(this.DEFAULT_SERVER_URL, path);

        int tries = 0;
        while (true)
            try {
                httpResponseString = Utils.executeHttpRequest(httpMethod, url, this.headers(), body);
                break;
            } catch (IOException e) {
                tries++;

                if (tries >= maxRetries)
                    throw e;

                int wait = new Random().nextInt(60);
                String msg = String.format("Communicating to Pixml (%s) timed out %d times, waiting ... %d seconds, error=%s", path, tries, wait, e.getMessage());
                System.out.println(msg);
                Thread.sleep(wait * 1000);
            }

        return handleResponse(httpResponseString);
    }

    private String getUrl(String default_server_url, String path) {
        return default_server_url + path;
    }

    private Map handleResponse(String response) throws JsonProcessingException {
        return mapper.readValue(response, Map.class);
    }

    /**
     * Performs a put request.
     * @param url An archivist URI path.
     * @param body The request body which will be serialized to json.
     * @return The http response object or an object deserialized from the response json if the ``json`` argument is true.
     * @throws IOException An error occurred making the request or parsing the JSON response
     * @throws InterruptedException
     */

    public Map put(String url, Map body) throws IOException, InterruptedException {

        return this.makeRequest("put", url, body);
    }
}
