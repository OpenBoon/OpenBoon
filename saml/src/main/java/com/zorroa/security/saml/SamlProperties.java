package com.zorroa.security.saml;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties("archivist.security.saml")
public class SamlProperties {

    public Map<String,String> keystore;
    public boolean discovery = true;
    public String baseUrl;
    public String landingPage;

    public Map<String, String> getKeystore() {
        return keystore;
    }

    public SamlProperties setKeystore(Map<String, String> keystore) {
        this.keystore = keystore;
        return this;
    }

    public boolean isDiscovery() {
        return discovery;
    }

    public SamlProperties setDiscovery(boolean discovery) {
        this.discovery = discovery;
        return this;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public SamlProperties setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public String getLandingPage() {
        return landingPage;
    }

    public SamlProperties setLandingPage(String landingPage) {
        this.landingPage = landingPage;
        return this;
    }
}
