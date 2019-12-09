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
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/*
 * PixmlClient is used to communicate to a Pixml API server.
 * */
public class PixmlClient {

    ObjectMapper mapper = new ObjectMapper();
    JsonNode apiKey;
    String server;

    String projectId;
    Integer maxRetries;

    private final String DEFAULT_SERVER_URL = "https://api.pixelml.com";

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
        this.server = Optional.ofNullable(server).orElse(DEFAULT_SERVER_URL);

    }

    public PixmlClient(Object apiKey) {
        this(apiKey, null);
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

    public Map<String, String> headers() {
        return this.headers("application/json");
    }

    public Map<String, String> headers(String contentType) {
        /*
        Generate the return some request headers.

        Args:
            content_type(str):  The content-type for the request. Defaults to
                'application/json'

        Returns:
            dict: An http header struct.
         */

        String authorization = null;
        try {
            authorization = "Bearer %s".format(signRequest());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
        Map header = new HashMap<String, String>();
        header.put("Authorization", authorization);

        Optional.ofNullable(contentType).ifPresent(
                (type) -> header.put("Content-Type", type));

        return header;

    }

    private String signRequest() throws JsonProcessingException {

        if (this.apiKey == null) {
            throw new RuntimeException("Unable to make request, no ApiKey has been specified.");
        }

        JWTCreator.Builder claimBuilder = JWT.create();

        claimBuilder.withClaim("aud", this.server);
        claimBuilder.withClaim("exp", Instant.now().plus(60, ChronoUnit.SECONDS).toEpochMilli());
        claimBuilder.withClaim("keyId", this.apiKey.get("keyId").textValue());

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


        String jwtEncoded = claimBuilder
                .sign(Algorithm
                        .HMAC512(this.apiKey.get("sharedKey").asText()));
        return jwtEncoded;
    }

    public Object post(String path, Map body, Boolean isJson) {

    /*
        """
        Performs a post request.

        Args:
            path (str): An archivist URI path.
            body (object): The request body which will be serialized to json.
            is_json (bool): Set to true to specify a JSON return value

        Returns:
            object: The http response object or an object deserialized from the
                response json if the ``json`` argument is true.

        Raises:
            Exception: An error occurred making the request or parsing the
                JSON response
        """
        return self._make_request('post', path, body, is_json)
     */

        isJson = Optional.ofNullable(isJson).orElse(true);

        return this.makeRequest("post", path, body, isJson);

    }

    private Map makeRequest(String httpMethod, String path, Map body, Boolean isJson) {
        String httpResponseString = "";

        try {
            httpResponseString = Utils.executeHttpRequest(httpMethod, path, this.headers(), body);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }


        try {
            Map<String, String> map = mapper.readValue(httpResponseString, Map.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }

        return null;
    }

    /*
    def _make_request(self, method, path, body=None, is_json=True):
        request_function = getattr(requests, method)
        if body is not None:
            data = json.dumps(body, cls=PixmlJsonEncoder)
        else:
            data = body

        # Making the request is wrapped in its own try/catch so it's easier
        # to catch any and all socket and http exceptions that can possibly be
        # thrown.  Once hat happens, handle_rsp is called which may throw
        # application level exceptions.
        rsp = None
        tries = 0
        url = self.get_url(path, body)
        while True:
            try:
                rsp = request_function(url, data=data, headers=self.headers(),
                                       verify=False)
                break
            except Exception as e:
                # Some form of connection error, wait until archivist comes
                # back.
                tries += 1
                if 0 < self.max_retries <= tries:
                    raise e
                wait = random.randint(1, random.randint(1, 60))
                # Switched to stderr in case no logger is setup, still want
                # to see messages.
                msg = "Communicating to Pixml (%s) timed out %d times, " \
                      "waiting ... %d seconds, error=%s\n"
                sys.stderr.write(msg % (url, tries, wait, e))
                time.sleep(wait)

        return self.__handle_rsp(rsp, is_json)
     */
}
