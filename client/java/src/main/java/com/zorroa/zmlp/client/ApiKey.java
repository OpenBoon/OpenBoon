package com.zorroa.zmlp.client;

public class ApiKey {

    private String secretKey;
    private String accessKey;

    public ApiKey() {
    }

    public ApiKey(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public ApiKey setSecretKey(String secretKey) {
        this.secretKey = secretKey;
        return this;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public ApiKey setAccessKey(String accessKey) {
        this.accessKey = accessKey;
        return this;
    }
}
