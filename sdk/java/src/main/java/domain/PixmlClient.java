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

/*
 * PixmlClient is used to communicate to a Pixml API server.
 * */
public class PixmlClient {

    ObjectMapper mapper = new ObjectMapper();
    JsonNode apiKey;
    String server;

    UUID projectId;
    Integer maxRetries;

    private final String DEFAULT_SERVER_URL = "https://api.pixelml.com";

    public PixmlClient(Object apiKey, String server, Map args) {
        /*
        Create a new PixmlClient instance.

        Args:
            apikey: An API key in any supported form. (dict, base64 string, or open file handle)
            server: The url of the server to connect to. Defaults to https://api.Pixml.zorroa.com
            project_id: An optional project UUID for API keys with access to multiple projects.
            max_retries: Maximum number of retries to make if the API
                server is down, 0 for unlimited.
        */

        args = Optional.ofNullable(args).orElse(new HashMap());

        this.apiKey = loadApiKey(apiKey);
        this.server = Optional.ofNullable(server).orElse(DEFAULT_SERVER_URL);
        this.maxRetries = (Integer) args.getOrDefault("max_retries", 3);
        this.projectId = (UUID) args.get("project_id");
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
        Optional.ofNullable(this.projectId).ifPresent((UUID projectId) -> {
            claimBuilder.withClaim("projectId", projectId.toString());
        });


        Algorithm sharedKey = Algorithm.HMAC512(this.apiKey.get("sharedKey").asText());
        String jwtEncoded = claimBuilder.sign(sharedKey);
        return jwtEncoded;
    }

    public Map post(String path, Map body) throws IOException, InterruptedException {

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


        return this.makeRequest("post", path, body);

    }

    private Map makeRequest(String httpMethod, String path, Map body) throws InterruptedException, IOException {

        String httpResponseString = null;
        String url = getUrl(this.DEFAULT_SERVER_URL,path);

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
        return default_server_url+path;
    }


    private Map handleResponse(String response) throws JsonProcessingException {
        return mapper.readValue(response, Map.class);
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


        def __handle_rsp(self, rsp, is_json):
        if rsp.status_code != 200:
            self.__raise_exception(rsp)
        if is_json and len(rsp.content):
            rsp_val = rsp.json()
            if logger.getEffectiveLevel() == logging.DEBUG:
                logger.debug(
                    "rsp: status: %d  body: '%s'" % (rsp.status_code, rsp_val))
            return rsp_val
        return rsp
     */
}
