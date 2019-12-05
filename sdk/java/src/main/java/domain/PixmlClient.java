package domain;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
                apiKeyString = (String)apiKey;

            //Check if is a valid JSON
            try {
                return mapper.readTree(apiKeyString);
            } catch (JsonProcessingException e) {
                System.out.println("Invalid Json Format");
            }
        }

        return null;
    }

    /*
        def headers(self, content_type="application/json"):
        """
        Generate the return some request headers.

        Args:
            content_type(str):  The content-type for the request. Defaults to
                'application/json'

        Returns:
            dict: An http header struct.

        """
        header = {'Authorization': "Bearer {}".format(self.__sign_request())}

        if content_type:
            header['Content-Type'] = content_type

        if logger.getEffectiveLevel() == logging.DEBUG:
            logger.debug("headers: %s" % header)

        return header
     */

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

    /*
        def __sign_request(self):
        if not self.apikey:
            raise RuntimeError("Unable to make request, no ApiKey has been specified.")
        claims = {
            'aud': self.server,
            'exp': datetime.datetime.utcnow() + datetime.timedelta(seconds=60),
            'keyId': self.apikey["keyId"],
        }

        if os.environ.get("PIXML_TASK_ID"):
            claims['taskId'] = os.environ.get("PIXML_TASK_ID")
            claims['jobId'] = os.environ.get("PIXML_JOB_ID")

        if self.project_id:
            claims["projectId"] = self.project_id
        return jwt.encode(claims, self.apikey['sharedKey'], algorithm='HS512').decode("utf-8")
     */

}
