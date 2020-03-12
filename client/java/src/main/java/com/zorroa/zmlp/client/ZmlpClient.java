package com.zorroa.zmlp.client;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.zmlp.client.domain.exception.ZmlpClientException;
import com.zorroa.zmlp.client.domain.asset.BatchUploadAssetsRequest;
import com.zorroa.zmlp.client.domain.exception.ZmlpRequestException;
import okhttp3.*;
import org.apache.http.client.HttpResponseException;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

        if (apiKey.getAccessKey() == null || apiKey.getSecretKey() == null) {
            throw new ZmlpClientException("No APIKey has been configured");
        }

        this.apiKey = apiKey;
        this.server = Optional.ofNullable(server).orElse(DEFAULT_SERVER_URL);
    }

    public <T> T get(String path, Object body, TypeReference<T> type) throws ZmlpRequestException {
        return marshallResponse(makeRequest(path, "get", body), type);
    }

    public <T> T get(String path, Object body, Class<T> type) throws ZmlpRequestException {
        return marshallResponse(makeRequest(path, "get", body), type);
    }

    public <T> T delete(String path, Object body, TypeReference<T> type) throws ZmlpRequestException {
        return marshallResponse(makeRequest(path, "delete", body), type);
    }

    public <T> T delete(String path, Object body, Class<T> type) throws ZmlpRequestException {
        return marshallResponse(makeRequest(path, "delete", body), type);
    }

    public <T> T put(String path, Object body, TypeReference<T> type) throws ZmlpRequestException {
        return marshallResponse(makeRequest(path, "put", body), type);
    }

    public <T> T put(String path, Object body, Class<T> type) throws ZmlpRequestException {
        return marshallResponse(makeRequest(path, "put", body), type);
    }

    public <T> T post(String path, Object body, TypeReference<T> type) throws ZmlpRequestException {
        return marshallResponse(makeRequest(path, "post", body), type);
    }

    public <T> T post(String path, Object body, Class<T> type) throws ZmlpRequestException {
        return marshallResponse(makeRequest(path, "post", body), type);
    }

    private String getUrl(String path) {
        if (server.endsWith("/")) {
            return server.concat(path.replaceFirst("/", ""));
        } else {
            return server.concat(path);
        }
    }

    public byte[] makeRequest(String path, String method, Object body) throws ZmlpRequestException {
        try {
            Request.Builder builder = new Request.Builder()
                    .url(getUrl(path));
            if (body == null) {
                builder.method(method.toUpperCase(), null);
            } else {
                builder.method(method.toUpperCase(),
                        RequestBody.create(JSON, Json.mapper.writeValueAsString(body)));
            }
            applyHeaders(builder);
            try (Response response = http.newCall(builder.build()).execute()) {
                if (response.code() != 200)
                    throw new ZmlpRequestException(response.body().string(),
                            path,
                            new HttpResponseException(response.code(), response.message()));

                return response.body().bytes();
            }
        } catch (IOException e) {
            String msg = method + " + request to " + path + " failed. " + e.getMessage();
            throw new ZmlpClientException(msg, e);
        }
    }

    private <T> T marshallResponse(byte[] response, TypeReference<T> type) throws ZmlpClientException {
        try {
            return Json.mapper.readValue(response, type);
        } catch (IOException e) {
            throw new ZmlpClientException("Could not  deserialize response", e);
        }
    }

    private <T> T marshallResponse(byte[] response, Class<T> type) throws ZmlpClientException {
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
        claimBuilder.withClaim("exp", Instant.now().plus(60, ChronoUnit.SECONDS).getEpochSecond());
        claimBuilder.withClaim("accessKey", apiKey.getAccessKey());
        Algorithm secretKey = Algorithm.HMAC512(apiKey.getSecretKey());
        builder.header("Authorization", "Bearer " + claimBuilder.sign(secretKey));
    }

    public <T> T uploadFiles(String path, BatchUploadAssetsRequest batchUploadAssetsRequest, Class<T> type) throws ZmlpClientException {
        return marshallResponse(multiPartFileUpload(path, batchUploadAssetsRequest), type);
    }


    private byte[] multiPartFileUpload(String path, BatchUploadAssetsRequest batchUploadAssetsRequest) throws ZmlpClientException {

        try {
            path = getUrl(path);
            MultipartBody.Builder multiPartBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);

            // Adding Files to Request
            batchUploadAssetsRequest.getAssets().forEach(value -> {
                File file = new File(value.getUri());

                RequestBody fileRequestBody = RequestBody.create(MediaType.get("application/octet-stream"), file);
                multiPartBuilder.addFormDataPart("files", file.getName(), fileRequestBody);
            });


            Map<String, Object> body = new HashMap();
            body.put("assets", batchUploadAssetsRequest.getAssets());
            body.put("modules", batchUploadAssetsRequest.getModules());
            multiPartBuilder.addFormDataPart("body", "", RequestBody.create(MediaType.get("application/json"), Json.asJson(batchUploadAssetsRequest)));


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
