package com.zorroa.zmlp.sdk;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.zmlp.sdk.domain.Asset.BatchCreateAssetResponse;
import com.zorroa.zmlp.sdk.domain.ZmlpClientException;
import okhttp3.*;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class ZmlpClient {

    public static final MediaType JSON = MediaType.parse("application/json");

    private final OkHttpClient http = new OkHttpClient();

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
    public ZmlpClient(ApiKey apiKey, String server) {
        if (apiKey == null) {
            throw new ZmlpClientException("No APIKey has been configured");
        }

        if (apiKey.getKeyId() == null || apiKey.getSharedKey() == null) {
            throw new ZmlpClientException("No APIKey has been configured");
        }

        this.apiKey = apiKey;
        this.server = Optional.ofNullable(server).orElse(DEFAULT_SERVER_URL);
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
        if (server.endsWith("/")) {
            return server.concat(path.replaceFirst("/", ""));
        } else {
            return server.concat(path);
        }
    }

    public byte[] makeRequest(String path, String method, Object body) {
        try {
            Request.Builder builder = new Request.Builder()
                    .url(getUrl(path));
            if (body == null) {
                builder.method(method, null);
            } else {
                builder.method(method.toUpperCase(),
                        RequestBody.create(JSON, Json.mapper.writeValueAsString(body)));
            }
            applyHeaders(builder);
            try (Response response = http.newCall(builder.build()).execute()) {
                return response.body().bytes();
            }
        } catch (IOException e) {
            String msg = method + " + request to " + path + " failed. " + e.getMessage();
            throw new ZmlpClientException(msg, e);
        }
    }

    private <T> T marshallResponse(byte[] response, TypeReference<T> type) {
        try {
            return Json.mapper.readValue(response, type);
        } catch (IOException e) {
            throw new ZmlpClientException("Could not  deserialize response", e);
        }
    }

    private <T> T marshallResponse(byte[] response, Class<T> type) {
        try {
            return Json.mapper.readValue(response, type);
        } catch (IOException e) {
            throw new ZmlpClientException("Could not  deserialize response ", e);
        }
    }

    private void applyHeaders(Request.Builder builder) {
        signRequest(builder);
    }

    private void signRequest(Request.Builder builder) {
        JWTCreator.Builder claimBuilder = JWT.create();
        claimBuilder.withClaim("aud", server);
        claimBuilder.withClaim("exp", Instant.now().plus(60, ChronoUnit.SECONDS).toEpochMilli());
        claimBuilder.withClaim("keyId", apiKey.getKeyId().toString());
        Algorithm sharedKey = Algorithm.HMAC512(apiKey.getSharedKey());
        builder.header("Authorization", "Bearer " + claimBuilder.sign(sharedKey));
    }

    public <T> T uploadFiles(String path, List<String> uris, Map body, Class<T> type) {
        return marshallResponse(multiPartFileUpload(path, uris, body), type);
    }

    private byte[] multiPartFileUpload(String path, List<String> uris, Map body) {

        try {
            path = getUrl(path);
            MultipartBody.Builder multiPartBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("body", Json.mapper.writeValueAsString(body));

            // Adding Files to Request
            uris.forEach(value -> {
                File file = new File(value);
                String contentType = new MimetypesFileTypeMap().getContentType(file);
                MediaType mimeType = MediaType.parse(contentType);
                RequestBody fileRequestBody = RequestBody.create(
                        mimeType, file);

                multiPartBuilder.addFormDataPart("files", file.getName(), fileRequestBody);

            });

            Request.Builder requestBuilder = new Request.Builder()
                    .url(path)
                    .post(multiPartBuilder.build());
            applyHeaders(requestBuilder);


            try (Response response = http.newCall(requestBuilder.build()).execute()) {
                return response.body().bytes();
            }

        } catch (IOException e) {
            String msg = "Multipart Request" + " + request to " + path + " failed. " + e.getMessage();
            throw new ZmlpClientException(msg, e);
        }
    }
}
