package com.zorroa.zmlp.sdk;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import okhttp3.*;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

public class ZmlpClient {

    public static final MediaType JSON = MediaType.parse("application/json");

    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final ApiKey apiKey;
    private final String server;

    private final String DEFAULT_SERVER_URL = "https://api.zmlp.zorroa.com";

    /**
     * ZmlpClient is used to communicate to a ZMLP API server.
     * Create a new ZmlpClient instance.
     *
     * @param apiKey An API key in any supported form. (dict, base64 string, or open file handle)
     * @param server The url of the server to connect to. Defaults to https://api.zmlp.zorroa.com
     */
    public ZmlpClient(Object apiKey, String server) {
        configureJsonMapper();
        this.apiKey = loadApiKey(apiKey);
        this.server = Optional.ofNullable(server).orElse(DEFAULT_SERVER_URL);
    }

    public ZmlpClient(Object apiKey) {
        this(apiKey, null);
    }

    public Boolean isApiKeySet() {
        return this.apiKey != null;
    }

    public <T> T get(String path, Object body, TypeReference<T> type) {
        return marshallResponse(makeRequest(path, "get", body), type);
    }

    public <T> T get(String path, Object body, Class<T> type) {
        return marshallResponse(makeRequest(path, "get", body), type);
    }

    public <T> T delete(String path, Object body, TypeReference<T> type) {
        return marshallResponse(makeRequest(path, "delete", body), type);
    }

    public <T> T delete(String path, Object body, Class<T> type) {
        return marshallResponse(makeRequest(path, "delete", body), type);
    }

    public <T> T put(String path, Object body, TypeReference<T> type) {
        return marshallResponse(makeRequest(path, "put", body), type);
    }

    public <T> T put(String path, Object body, Class<T> type) {
        return marshallResponse(makeRequest(path, "put", body), type);
    }

    public <T> T post(String path, Object body, TypeReference<T> type) {
        return marshallResponse(makeRequest(path, "post", body), type);
    }

    public <T> T post(String path, Object body, Class<T> type) {
        return marshallResponse(makeRequest(path, "post", body), type);
    }

    private String getUrl(String path) {
        return (server + ("/" + path).replace("//", "/"));
    }

    public InputStream makeRequest(String path, String method, Object body) {
        try {
            Request.Builder builder = new Request.Builder()
                    .url(getUrl(path));
            if (body == null) {
                builder.method(method, null);
            } else {
                builder.method(method.toUpperCase(),
                        RequestBody.create(mapper.writeValueAsString(body), JSON));
            }
            applyHeaders(builder);

            try (Response response = http.newCall(builder.build()).execute()) {
                return response.body().byteStream();
            }
        } catch (IOException e) {
            String msg = method + " + request to " + path + " failed. " + e.getMessage();
            throw new ZmlpClientException(msg, e);
        }
    }

    private <T> T marshallResponse(InputStream response, TypeReference<T> type) {
        try {
            return mapper.readValue(response, type);
        } catch (IOException e) {
            throw new ZmlpClientException("Could not  deserialize response", e);
        }
    }


    private <T> T marshallResponse(InputStream response, Class<T> type) {
        try {
            return mapper.readValue(response, type);
        } catch (IOException e) {
            throw new ZmlpClientException("Could not  deserialize response", e);
        }
    }

    private ApiKey loadApiKey(Object apiKey) {

        if (apiKey instanceof Map) {
            return mapper.convertValue(apiKey, ApiKey.class);
        } else if (apiKey instanceof String) {
            byte[] decoded = Base64.getDecoder().decode(apiKey.toString());
            try {
                return mapper.convertValue(decoded, ApiKey.class);
            } catch (Exception e) {
                throw new ZmlpClientException("Failed to parse API Key", e);
            }
        } else {
            return null;
        }
    }

    private void applyHeaders(Request.Builder builder) {
        // The content type is already set.
        builder.addHeader("Authorization", String.format("Bearer %s", signRequest()));
    }

    private String signRequest() {
        if (this.apiKey == null) {
            throw new RuntimeException("Unable to make request, no ApiKey has been specified.");
        }

        JWTCreator.Builder claimBuilder = JWT.create();
        claimBuilder.withClaim("aud", this.server);
        claimBuilder.withClaim("exp", Instant.now().plus(60, ChronoUnit.SECONDS).toEpochMilli());
        claimBuilder.withClaim("keyId", this.apiKey.getKeyId().toString());
        ;
        Algorithm sharedKey = Algorithm.HMAC512(apiKey.getSigningKey());
        return claimBuilder.sign(sharedKey);
    }

    private void configureJsonMapper() {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_INDEX, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.configure(MapperFeature.USE_GETTERS_AS_SETTERS, false);
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
    }
}
