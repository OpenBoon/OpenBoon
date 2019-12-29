package com.zorroa.zmlp.sdk;

import java.util.UUID;

public class ApiKey {

    private String sharedKey;
    private UUID keyId;

    public ApiKey() { }

    public ApiKey(UUID keyId, String sharedKey) {
        this.keyId = keyId;
        this.sharedKey = sharedKey;
    }

    public String getSharedKey() {
        return sharedKey;
    }

    public void setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;
    }

    public UUID getKeyId() {
        return keyId;
    }

    public void setKeyId(UUID keyId) {
        this.keyId = keyId;
    }
}
