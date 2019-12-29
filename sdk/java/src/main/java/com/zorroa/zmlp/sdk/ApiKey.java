package com.zorroa.zmlp.sdk;

import java.util.UUID;

public class ApiKey {

    private String signingKey;
    private UUID keyId;

    public String getSigningKey() {
        return signingKey;
    }

    public void setSigningKey(String signingKey) {
        this.signingKey = signingKey;
    }

    public UUID getKeyId() {
        return keyId;
    }

    public void setKeyId(UUID keyId) {
        this.keyId = keyId;
    }
}
